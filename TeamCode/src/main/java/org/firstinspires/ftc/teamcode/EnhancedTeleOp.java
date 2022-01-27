package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.util.ProfileTrapezoidal;

import java.util.List;

public class EnhancedTeleOp extends OpMode {

    private final ElapsedTime runtime = new ElapsedTime();
    private DcMotorEx rearRightDrive = null;
    private DcMotorEx rearLeftDrive = null;
    private DcMotorEx frontRightDrive = null;
    private DcMotorEx frontLeftDrive = null;
    private final DcMotorEx[] driveMotors = new DcMotorEx[4];
    private DcMotorEx armLift;
    private DcMotor clawLeft;
    private DcMotor clawRight;
    private double armTargetRaw;
    private Manipulator.ArmPosition lastArmPosition = Manipulator.ArmPosition.UNKNOWN;
    private Manipulator.ArmPosition armPosition = Manipulator.ArmPosition.UNKNOWN;
    private ProfileTrapezoidal trap;
    private ElapsedTime dt = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
    @Override
    public void init() {
        telemetry.addData("Status", "Initializing");

        List<LynxModule> allHubs = hardwareMap.getAll(LynxModule.class);

        for (LynxModule hub : allHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }
        // Initialize the hardware variables. Note that the strings used here as parameters
        // to 'get' must correspond to the names assigned during the robot configuration
        // step (using the FTC Robot Controller app on the phone).
        // get DcMotorEx or get DcMotor and cast to DcMotorEx
        driveMotors[0] = rearRightDrive  = hardwareMap.get(DcMotorEx.class, "rear_right_drive");
        driveMotors[1] = rearLeftDrive = hardwareMap.get(DcMotorEx.class, "rear_left_drive");
        driveMotors[2] = frontRightDrive  = hardwareMap.get(DcMotorEx.class, "front_right_drive");
        driveMotors[3] = frontLeftDrive = hardwareMap.get(DcMotorEx.class, "front_left_drive");

        armLift = hardwareMap.get(DcMotorEx.class, "arm_lift");
        armLift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        armLift.setDirection(DcMotor.Direction.FORWARD);
        armLift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        armLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        // TODO tuning! leaving these commented to start since Caleb reports the setpoint *is* being reached; the defaults are ok?
        // need to set both velocity and position coefficients since the position controller just sets velocity goals
        //armLift.setVelocityPIDFCoefficients(1.0, 0.1, 0.1, 0.1) // p, i, d, f
        //armLift.setPositionPIDFCoefficients(1.0) // p

        trap = new ProfileTrapezoidal(2000, 4000); // cruise speed, acceleration TODO adjust these
        dt.reset();

        clawLeft = hardwareMap.get(DcMotor.class, "claw_left");
        clawRight = hardwareMap.get(DcMotor.class, "claw_right");
        clawLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        clawRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        clawRight.setDirection(DcMotor.Direction.REVERSE);

        for (DcMotorEx motor : driveMotors) {
            motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }

        // Most robots need the motor on one side to be reversed to drive forward
        // Reverse the motor that runs backwards when connected directly to the battery
        rearRightDrive.setDirection(DcMotor.Direction.REVERSE);
        rearLeftDrive.setDirection(DcMotor.Direction.FORWARD);
        frontRightDrive.setDirection(DcMotor.Direction.REVERSE);
        frontLeftDrive.setDirection(DcMotor.Direction.FORWARD);

        // Tell the driver that initialization is complete.
        telemetry.addData("Status", "Initialized");
    }

    @Override
    public void loop() {
        int armEncoder = armLift.getCurrentPosition();

        double fastMode = gamepad1.left_stick_button ? 0.85 : 0.60;
        if (fastMode > 0.60) { // move arm up if fast mode is on
            armLift.setTargetPosition(-300);
            armLift.setPower(0.6);
            armLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }

        // POV Mode uses left stick to go forward, and right stick to turn.
        // - This uses basic math to combine motions and is easier to drive straight.
        double drive = -Math.pow(gamepad1.left_stick_y, 2);
        double turn  =  Math.pow(gamepad1.right_stick_x, 2);
        // max speed is 165 rpm according to TetrixMotor.java. velocity is in rpm
        double leftVelocity = Range.scale(drive + turn, -2, 2, -165, 165);
        double rightVelocity = Range.scale(drive - turn, -2, 2, -165, 165);
        // convert rotations into degrees
        leftVelocity *= 360;
        rightVelocity *= 360;

        // Scale by "fast mode" and send power
        rearLeftDrive.setVelocity(leftVelocity*fastMode, AngleUnit.DEGREES);
        frontLeftDrive.setVelocity(leftVelocity*fastMode, AngleUnit.DEGREES);
        rearRightDrive.setVelocity(rightVelocity*fastMode, AngleUnit.DEGREES);
        frontRightDrive.setVelocity(rightVelocity*fastMode, AngleUnit.DEGREES);


        // Show the elapsed game time and wheel power.
        telemetry.addData("Status", "Run Time: " + runtime);
        telemetry.addData("Desired", "left (%.2f), right (%.2f)", leftVelocity, rightVelocity);
        telemetry.addData("Real", "RL (%.2f), RR (%.2f), FL (%.2f), FR (%.2f)", rearLeftDrive.getVelocity(), rearRightDrive.getVelocity(), frontLeftDrive.getVelocity(), frontRightDrive.getVelocity());
        telemetry.addData("arm pos", armEncoder);

        // both triggers = duck mode
        if (gamepad1.right_trigger > 0.00 && gamepad1.left_trigger > 0.00) {
            clawLeft.setPower(gamepad1.left_trigger);
            clawRight.setPower(-gamepad1.right_trigger);
            armPosition = Manipulator.ArmPosition.DUCK; // handle later

        } else if (gamepad1.right_trigger > 0.0){ // right is out, positive is out
            clawLeft.setPower(gamepad1.right_trigger);
            clawRight.setPower(gamepad1.right_trigger);

        } else if (gamepad1.left_trigger > 0.0) { // left is in, negative is in
            clawLeft.setPower(-gamepad1.left_trigger);
            clawRight.setPower(-gamepad1.left_trigger);

        } else { // halt motors if no buttons are pressed
            clawLeft.setPower(0);
            clawRight.setPower(0);
        }

        char button = getGamepadButtons(gamepad1);
        telemetry.addData("button", button);
        switch (button) {
            case 'a':
                armPosition = Manipulator.ArmPosition.GROUND;
                armTargetRaw = -30;
                break;
            case 'b':
                armPosition = Manipulator.ArmPosition.BOTTOM;
                armTargetRaw = -300;
                break;
            case 'y':
                armPosition = Manipulator.ArmPosition.MIDDLE_TELEOP;
                armTargetRaw = -600;
                break;
            case 'x':
                armPosition = Manipulator.ArmPosition.TOP;
                armTargetRaw = -915;
                break;
            case 'd':
                armTargetRaw += 30;
                break;
            case 'u':
                armTargetRaw -= 30;
                break;
        }


        if (armPosition == Manipulator.ArmPosition.DUCK && lastArmPosition != Manipulator.ArmPosition.DUCK) { // transition to duck mode
            armTargetRaw = -680;
        }
        telemetry.addData("armTargetRaw", armTargetRaw);
        telemetry.addData("arm state", armPosition);

        lastArmPosition = armPosition;

        // profile and move motor
        double armTargetSmooth = trap.smooth(armTargetRaw, dt.time());
        dt.reset();

        armLift.setTargetPosition((int) armTargetSmooth);
        telemetry.addData("armTargetSmooth", armTargetSmooth);
    }
    private char getGamepadButtons(Gamepad gamepad) {
        if (gamepad.a) {
            return 'a';
        } else if (gamepad.b) {
            return 'b';
        } else if (gamepad.x) {
            return 'x';
        } else if (gamepad.y) {
            return 'y';
        } else if (gamepad.dpad_down) {
            return 'd';
        } else if (gamepad.dpad_up) {
            return 'u';
        } else
            return '\u0000';
    }

}