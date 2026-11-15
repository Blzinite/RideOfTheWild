package net.blzinite.horsectrl.util;

public interface HorseControlAccess {
    float horseCtrl$getLean();
    void horseCtrl$setLean(float lean);
    boolean horseCtrl$hasRealSaddle();
    void horseCtrl$receiveInput(float forward, float right, float yaw, boolean sprintPressed);
}
