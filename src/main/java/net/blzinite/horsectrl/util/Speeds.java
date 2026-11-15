package net.blzinite.horsectrl.util;

public enum Speeds {
    WALK(1),
    TROT(2),
    GALLOP(3),
    SPRINT(4);

    private final int value;
    Speeds(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
    public static Speeds fromValue(int value) {
        return switch (value) {
            case 1 -> WALK;
            case 2 -> TROT;
            case 4 -> SPRINT;
            default -> GALLOP;
        };
    }
}
