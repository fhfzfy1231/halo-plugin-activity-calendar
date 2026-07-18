package team.foxbridge.activitycalendar;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import run.halo.app.content.ContentWrapper;
import run.halo.app.content.PostContentService;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.SinglePage;
import run.halo.app.core.extension.content.Snapshot;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.SettingFetcher;

/** Tracks real content deltas without counting repeated auto-saves as separate actions. */
@Component
public class ActivityTracker {

    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern CODE_BLOCKS = Pattern.compile(
        "<(pre|code)\\b[^>]*>.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern UNITS = Pattern.compile("[\\p{IsHan}]|[\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Comparator<Post> POST_ORDER =
        Comparator.comparing(post -> post.getMetadata().getName());
    private static final Comparator<SinglePage> PAGE_ORDER =
        Comparator.comparing(page -> page.getMetadata().getName());

    private final ReactiveExtensionClient client;
    private final PostContentService postContentService;
    private final SettingFetcher settingFetcher;
    private final Map<String, ContentState> states = new ConcurrentHashMap<>();
    private Disposable trackingTask;

    public ActivityTracker(ReactiveExtensionClient client, PostContentService postContentService,
        SettingFetcher settingFetcher) {
        this.client = client;
        this.postContentService = postContentService;
        this.settingFetcher = settingFetcher;
    }

    public synchronized void startTracking() {
        if (trackingTask != null && !trackingTask.isDisposed()) {
            return;
        }
        trackingTask = scanAll(true)
            .thenMany(Flux.interval(Duration.ofSeconds(15)).concatMap(ignored -> scanAll(false)))
            .onErrorContinue((error, value) -> { })
            .subscribe();
    }

    public synchronized void stopTracking() {
        if (trackingTask != null) {
            trackingTask.dispose();
            trackingTask = null;
        }
        states.clear();
    }

    private Mono<Void> scanAll(boolean baselineOnly) {
        Flux<ContentDescriptor> posts = client.list(Post.class, post -> true, POST_ORDER)
            .map(post -> new ContentDescriptor(
                "post/" + post.getMetadata().getName(),
                post.getMetadata().getName(),
                post.getSpec().getOwner(),
                post.getSpec().getHeadSnapshot(),
                post.getSpec().getBaseSnapshot(),
                post.getSpec().getReleaseSnapshot(),
                Boolean.TRUE.equals(post.getSpec().getPublish()),
                false));
        Flux<ContentDescriptor> pages = client.list(SinglePage.class, page -> true, PAGE_ORDER)
            .map(page -> new ContentDescriptor(
                "page/" + page.getMetadata().getName(),
                page.getMetadata().getName(),
                page.getSpec().getOwner(),
                page.getSpec().getHeadSnapshot(),
                page.getSpec().getBaseSnapshot(),
                page.getSpec().getReleaseSnapshot(),
                Boolean.TRUE.equals(page.getSpec().getPublish()),
                true));
        return Flux.concat(posts, pages)
            .concatMap(descriptor -> inspect(descriptor, baselineOnly))
            .then();
    }

    private Mono<Void> inspect(ContentDescriptor descriptor, boolean baselineOnly) {
        ContentState previous = states.get(descriptor.key());
        if (previous != null
            && Objects.equals(previous.headSnapshot(), descriptor.headSnapshot())
            && Objects.equals(previous.releaseSnapshot(), descriptor.releaseSnapshot())
            && previous.published() == descriptor.published()) {
            return Mono.empty();
        }

        return loadContent(descriptor)
            .flatMap(content -> loadContributor(descriptor).map(user ->
                new LoadedContent(normalize(content.getContent()), user.username(), user.displayName())))
            .flatMap(loaded -> {
                ContentState current = new ContentState(loaded.text(), descriptor.headSnapshot(),
                    descriptor.releaseSnapshot(), descriptor.published());
                states.put(descriptor.key(), current);
                if (baselineOnly || previous == null) {
                    return Mono.empty();
                }
                TextDelta delta = calculateDelta(previous.text(), current.text());
                int published = !previous.published() && current.published() ? 1 : 0;
                int republished = previous.published() && current.published()
                    && !Objects.equals(previous.releaseSnapshot(), current.releaseSnapshot()) ? 1 : 0;
                if (delta.added() == 0 && delta.modified() == 0
                    && published == 0 && republished == 0) {
                    return Mono.empty();
                }
                return addActivity(loaded.username(), loaded.displayName(), delta.added(),
                    delta.modified(), published, republished);
            })
            .onErrorResume(error -> Mono.empty());
    }

    private Mono<ContentWrapper> loadContent(ContentDescriptor descriptor) {
        if (!descriptor.page()) {
            return postContentService.getHeadContent(descriptor.name());
        }
        if (isBlank(descriptor.headSnapshot()) || isBlank(descriptor.baseSnapshot())) {
            return Mono.empty();
        }
        return client.fetch(Snapshot.class, descriptor.baseSnapshot())
            .flatMap(base -> {
                if (Objects.equals(descriptor.headSnapshot(), descriptor.baseSnapshot())) {
                    return Mono.just(ContentWrapper.patchSnapshot(base, base));
                }
                return client.fetch(Snapshot.class, descriptor.headSnapshot())
                    .map(head -> ContentWrapper.patchSnapshot(head, base));
            });
    }

    private Mono<Contributor> loadContributor(ContentDescriptor descriptor) {
        Mono<String> username = isBlank(descriptor.headSnapshot())
            ? Mono.justOrEmpty(descriptor.owner())
            : client.fetch(Snapshot.class, descriptor.headSnapshot())
                .map(Snapshot::getSpec)
                .map(Snapshot.SnapShotSpec::getOwner)
                .filter(owner -> !isBlank(owner))
                .switchIfEmpty(Mono.justOrEmpty(descriptor.owner()));
        return username.defaultIfEmpty("unknown")
            .flatMap(name -> client.fetch(User.class, name)
                .map(user -> new Contributor(name,
                    isBlank(user.getSpec().getDisplayName()) ? name : user.getSpec().getDisplayName()))
                .defaultIfEmpty(new Contributor(name, name)));
    }

    private Mono<Void> addActivity(String username, String displayName, long added, long modified,
        int published, int republished) {
        String date = LocalDate.now(ZoneId.systemDefault()).toString();
        String recordName = date.replace("-", "") + "-" + shortHash(username);
        ActivitySettings settings = settings();
        long score = Math.round(added * settings.getAddedWeight()
            + modified * settings.getModifiedWeight()
            + (long) published * settings.getPublishScore()
            + (long) republished * settings.getRepublishScore());

        return Mono.defer(() -> client.fetch(ActivityRecord.class, recordName)
                .flatMap(record -> {
                    ActivityRecord.Spec spec = record.getSpec();
                    spec.setDisplayName(displayName);
                    spec.setAddedWords(spec.getAddedWords() + added);
                    spec.setModifiedWords(spec.getModifiedWords() + modified);
                    spec.setPublishedCount(spec.getPublishedCount() + published);
                    spec.setRepublishedCount(spec.getRepublishedCount() + republished);
                    spec.setScore(spec.getScore() + score);
                    return client.update(record);
                })
                .switchIfEmpty(Mono.defer(() -> client.create(newRecord(recordName, date,
                    username, displayName, added, modified, published, republished, score)))))
            .retryWhen(Retry.backoff(4, Duration.ofMillis(100)))
            .then();
    }

    private ActivityRecord newRecord(String name, String date, String username, String displayName,
        long added, long modified, int published, int republished, long score) {
        ActivityRecord record = new ActivityRecord();
        Metadata metadata = new Metadata();
        metadata.setName(name);
        metadata.setLabels(Map.of("plugin.halo.run/plugin-name", "activity-calendar"));
        record.setMetadata(metadata);
        ActivityRecord.Spec spec = new ActivityRecord.Spec();
        spec.setDate(date);
        spec.setUsername(username);
        spec.setDisplayName(displayName);
        spec.setAddedWords(added);
        spec.setModifiedWords(modified);
        spec.setPublishedCount(published);
        spec.setRepublishedCount(republished);
        spec.setScore(score);
        record.setSpec(spec);
        return record;
    }

    ActivitySettings settings() {
        return settingFetcher.fetch("basic", ActivitySettings.class)
            .orElseGet(ActivitySettings::new);
    }

    static TextDelta calculateDelta(String before, String after) {
        if (Objects.equals(before, after)) {
            return new TextDelta(0, 0);
        }
        int prefix = 0;
        int min = Math.min(before.length(), after.length());
        while (prefix < min && before.charAt(prefix) == after.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < min - prefix
            && before.charAt(before.length() - 1 - suffix)
            == after.charAt(after.length() - 1 - suffix)) {
            suffix++;
        }
        String oldMiddle = before.substring(prefix, before.length() - suffix);
        String newMiddle = after.substring(prefix, after.length() - suffix);
        long oldUnits = countUnits(oldMiddle);
        long newUnits = countUnits(newMiddle);
        return new TextDelta(Math.max(0, newUnits - oldUnits), Math.min(oldUnits, newUnits));
    }

    static long countUnits(String value) {
        Matcher matcher = UNITS.matcher(value == null ? "" : value);
        long count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    static String normalize(String html) {
        if (html == null) {
            return "";
        }
        return TAGS.matcher(CODE_BLOCKS.matcher(html).replaceAll(" "))
            .replaceAll(" ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String shortHash(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 8);
        } catch (Exception e) {
            return Integer.toUnsignedString(input.hashCode(), 36);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record ContentDescriptor(String key, String name, String owner, String headSnapshot,
                             String baseSnapshot, String releaseSnapshot, boolean published,
                             boolean page) { }
    record ContentState(String text, String headSnapshot, String releaseSnapshot,
                        boolean published) { }
    record LoadedContent(String text, String username, String displayName) { }
    record Contributor(String username, String displayName) { }
    record TextDelta(long added, long modified) { }
}
