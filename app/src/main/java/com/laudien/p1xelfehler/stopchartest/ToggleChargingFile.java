package com.laudien.p1xelfehler.stopchartest;

class ToggleChargingFile {
    private String path, chargeOn, chargeOff;

    ToggleChargingFile(String path, String chargeOn, String chargeOff) {
        this.path = path;
        this.chargeOn = chargeOn;
        this.chargeOff = chargeOff;
    }

    public String getPath() {
        return path;
    }

    public String getChargeOn() {
        return chargeOn;
    }

    public String getChargeOff() {
        return chargeOff;
    }
}
