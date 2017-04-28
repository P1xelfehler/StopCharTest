package com.laudien.p1xelfehler.stopchartest;

class ToggleChargingFile {
    private String path, chargeOn, chargeOff;

    ToggleChargingFile(String path, String chargeOn, String chargeOff) {
        this.path = path;
        this.chargeOn = chargeOn;
        this.chargeOff = chargeOff;
    }

    String getPath() {
        return path;
    }

    String getChargeOn() {
        return chargeOn;
    }

    String getChargeOff() {
        return chargeOff;
    }
}
