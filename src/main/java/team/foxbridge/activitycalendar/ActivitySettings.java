package team.foxbridge.activitycalendar;

public class ActivitySettings {
    private double addedWeight = 1.0;
    private double modifiedWeight = 0.5;
    private int publishScore = 800;
    private int republishScore = 200;
    private int level1 = 1;
    private int level2 = 200;
    private int level3 = 600;
    private int level4 = 1500;

    public double getAddedWeight() { return addedWeight; }
    public void setAddedWeight(double addedWeight) { this.addedWeight = addedWeight; }
    public double getModifiedWeight() { return modifiedWeight; }
    public void setModifiedWeight(double modifiedWeight) { this.modifiedWeight = modifiedWeight; }
    public int getPublishScore() { return publishScore; }
    public void setPublishScore(int publishScore) { this.publishScore = publishScore; }
    public int getRepublishScore() { return republishScore; }
    public void setRepublishScore(int republishScore) { this.republishScore = republishScore; }
    public int getLevel1() { return level1; }
    public void setLevel1(int level1) { this.level1 = level1; }
    public int getLevel2() { return level2; }
    public void setLevel2(int level2) { this.level2 = level2; }
    public int getLevel3() { return level3; }
    public void setLevel3(int level3) { this.level3 = level3; }
    public int getLevel4() { return level4; }
    public void setLevel4(int level4) { this.level4 = level4; }
}
