package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;
import za.co.entelect.challenge.enums.Profession;

public class MyWorm extends Worm {
    @SerializedName("weapon")
    public Weapon weapon;

    @SerializedName("bananBomb")
    public BananaBomb bananaBomb;

    @SerializedName("snowBall")
    public SnowBall snowBall;
}
