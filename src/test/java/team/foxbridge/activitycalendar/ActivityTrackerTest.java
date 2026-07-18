package team.foxbridge.activitycalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActivityTrackerTest {

    @Test
    void countsChineseAndEnglishUnits() {
        assertEquals(4, ActivityTracker.countUnits("中文 test 123"));
    }

    @Test
    void countsAppendedTextAsAddedWords() {
        var delta = ActivityTracker.calculateDelta("已有内容", "已有内容新增三字");
        assertEquals(4, delta.added());
        assertEquals(0, delta.modified());
    }

    @Test
    void countsReplacementAsModification() {
        var delta = ActivityTracker.calculateDelta("火箭发射成功", "火箭测试成功");
        assertTrue(delta.modified() > 0);
    }

    @Test
    void stripsHtmlBeforeCounting() {
        assertEquals("标题 正文", ActivityTracker.normalize("<h1>标题</h1><p>正文</p>"));
    }

    @Test
    void ignoresCodeBlocks() {
        assertEquals("正文", ActivityTracker.normalize("<p>正文</p><pre><code>ignored()</code></pre>"));
    }
}
