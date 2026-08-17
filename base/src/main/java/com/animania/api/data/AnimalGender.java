package com.animania.api.data;

public enum AnimalGender {
    MALE,
    FEMALE,
    CHILD,
    /** Legacy genderless animals such as amphibians and solitary rodents. */
    NONE;

    public boolean isAdult() {
        return this == MALE || this == FEMALE;
    }
}
