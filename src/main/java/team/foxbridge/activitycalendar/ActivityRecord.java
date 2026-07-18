package team.foxbridge.activitycalendar;

import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@GVK(group = "activity.foxbridge.team", version = "v1alpha1", kind = "ActivityRecord",
    plural = "activityrecords", singular = "activityrecord")
public class ActivityRecord extends AbstractExtension {

    private Spec spec = new Spec();

    public Spec getSpec() {
        return spec;
    }

    public void setSpec(Spec spec) {
        this.spec = spec;
    }

    public static class Spec {
        private String date;
        private String username;
        private String displayName;
        private long addedWords;
        private long modifiedWords;
        private int publishedCount;
        private int republishedCount;
        private long score;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public long getAddedWords() { return addedWords; }
        public void setAddedWords(long addedWords) { this.addedWords = addedWords; }
        public long getModifiedWords() { return modifiedWords; }
        public void setModifiedWords(long modifiedWords) { this.modifiedWords = modifiedWords; }
        public int getPublishedCount() { return publishedCount; }
        public void setPublishedCount(int publishedCount) { this.publishedCount = publishedCount; }
        public int getRepublishedCount() { return republishedCount; }
        public void setRepublishedCount(int republishedCount) { this.republishedCount = republishedCount; }
        public long getScore() { return score; }
        public void setScore(long score) { this.score = score; }
    }
}
