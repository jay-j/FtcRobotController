package org.firstinspires.ftc.teamcode.test;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.Manipulator;

@Config
@Autonomous
public class ManipulatorTest extends LinearOpMode {
    public static int ARM_POSITION = 0;
    public static int ARM_POWER = 0;
    public static int CLAW_LEFT_POWER = 0;
    public static int CLAW_RIGHT_POWER = 0;
    @Override
    public void runOpMode() throws InterruptedException {
        FtcDashboard dashboard = FtcDashboard.getInstance();
        Manipulator manipulator = new Manipulator(
                hardwareMap.dcMotor.get("arm_lift"),
                hardwareMap.dcMotor.get("claw_left"),
                hardwareMap.dcMotor.get("claw_right")
        );

        waitForStart();
        if (isStopRequested()) return;
        while (opModeIsActive()) {
            manipulator.moveArmToPosition(ARM_POSITION, ARM_POWER);
            manipulator.setClawLeftPower(CLAW_LEFT_POWER);
            manipulator.setClawRightPower(CLAW_RIGHT_POWER);
        }

    }
}