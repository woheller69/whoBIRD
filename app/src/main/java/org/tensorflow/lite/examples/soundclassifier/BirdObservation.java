package org.tensorflow.lite.examples.soundclassifier;

public class BirdObservation {
    private int id;
    private long millis;
    private float latitude;
    private float longitude;
    private String name;
    private int speciesId;
    private float probability;

    public BirdObservation(int id, long millis, float latitude, float longitude, String name, int speciesId, float probability) {
        this.id = id;
        this.millis = millis;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.speciesId = speciesId;
        this.probability = probability;
    }

    public BirdObservation() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(int speciesId) {
        this.speciesId = speciesId;
    }

    public float getProbability() {
        return probability;
    }

    public void setProbability(float probability) {
        this.probability = probability;
    }
}