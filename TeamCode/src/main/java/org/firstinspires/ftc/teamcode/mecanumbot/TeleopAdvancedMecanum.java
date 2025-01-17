/*
Copyright 2019 FIRST Tech Challenge Team 12886

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package org.firstinspires.ftc.teamcode.mecanumbot;

import android.app.Activity;
import android.view.View;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

import java.text.DecimalFormat;

/**
 * This file contains an example of an iterative (Non-Linear) "OpMode".
 * An OpMode is a 'program' that runs in either the autonomous or the teleop period of an FTC match.
 * The names of OpModes appear on the menu of the FTC Driver Station.
 * When an selection is made from the menu, the corresponding OpMode
 * class is instantiated on the Robot Controller and executed.
 *
 * Remove a @Disabled the on the next line or two (if present) to add this opmode to the Driver Station OpMode list,
 * or add a @Disabled annotation to prevent this OpMode from being added to the Driver Station
 */
@TeleOp

public class TeleopAdvancedMecanum extends OpMode {
    // CONFIGURATION

    // Expansion Hub 1:
    // Motors:
    // Port 0: manArm
    // Servos
    // Port 0: leHand <-- Still needed
    // Sensors:
    // I2C Bus 0: Expansion Hub IMU
    // I2C Bus 1: frontDistance

    // Expansion Hub 2:
    // Motors:
    // Port 0: frontLeft
    // Port 1: rearLeft
    // Port 2: frontRight
    // Port 3: rearRight

    public DecimalFormat format = new DecimalFormat("##.00");
    private BNO055IMU imu;

    private DcMotor manArm;
    private boolean rightBumperDown = false;
    private boolean leftBumperDown = false;
    private boolean rightStickClick = false;

    private Servo elbow;
    private Servo hand;
    
    private Servo leftFound;
    private Servo rightFound;

    private DistanceSensor frontDistance;
    private ColorSensor frontColor;
    private DistanceSensor frontRange;
    private DistanceSensor rearDist;
    private DistanceSensor leftDist;
    private DistanceSensor rightDist;
    private String armState = "0block"; // <-- Using string comparators to better explain...
                                        // ...what is happening with the armState
    private final String driveState = "step1";

    public Orientation angles;
    public Acceleration gravity;

    private boolean arcadeMode = false;
    private boolean manualControl = false;
    //remove? Is not used
    private final int gyroCalibratedCount = 0;
    
    private float manualArmPower;

    private MecanumDriveAdvanced mecanum;

    private ElapsedTime time;

        // hsvValues is an array that will hold the hue, saturation, and value information.
    private final float[] hsvValues = {0F, 0F, 0F};

      private final float[] values = hsvValues;

        // sometimes it helps to multiply the raw RGB values with a scale factor
        // to amplify/attentuate the measured values.
    private final double SCALE_FACTOR = 255;
    private View relativeLayout;
    private int relativeLayoutId;

    @Override
    public void init() {
      armState = "0block";
      motorInit();
      gyroInit();
      sensorInit();
      time = new ElapsedTime();
      time.reset();
    }

    @Override
    public void init_loop() {
      gyroLoop();
    }

    @Override
    public void loop() {
      telemetry.addData("loop time", time.milliseconds());        
      time.reset();
      gyroLoop();
      mecanum.go();

      foundGrab();
      selectPosition();
      moveArm();
      dropBlock(); // servo comm,
    }

    private void gamepadInit() {
      //gamepad1.setJoystickDeadzone(0.2f);
    }

    private void motorInit() {
      mecanum = new MecanumDriveAdvanced();
      mecanum.init(gamepad1, hardwareMap);
      
      leftFound = hardwareMap.get(Servo.class, "leftFound");
      rightFound = hardwareMap.get(Servo.class, "rightFound");

      manArm = hardwareMap.get(DcMotor.class, "manArm");
      hand = hardwareMap.get(Servo.class, "leHand");

      manArm.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
      manArm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
      manArm.setMode(DcMotor.RunMode.RUN_USING_ENCODER); // TODO IS THIS THE RIGHT MODE?! SEE RUN_TO_POSITION
    }

    private void gyroInit() {
      imu = hardwareMap.get(BNO055IMU.class, "imu");
      BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();

      parameters.mode                = BNO055IMU.SensorMode.IMU;
      parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
      parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
      parameters.loggingEnabled      = false;

      imu.initialize(parameters);
    }

    private void sensorInit() {
      frontDistance = hardwareMap.get(DistanceSensor.class, "frontDistance");
      frontColor = hardwareMap.get(ColorSensor.class, "frontDistance");
      frontRange = hardwareMap.get(DistanceSensor.class, "frontDistyboi");
      leftDist = hardwareMap.get(DistanceSensor.class, "leftDist");
      rightDist = hardwareMap.get(DistanceSensor.class, "rightDist");
      rearDist = hardwareMap.get(DistanceSensor.class, "rearDist");


      // values is a reference to the hsvValues array.
      float[] values = hsvValues;


      // get a reference to the RelativeLayout so we can change the background
      // color of the Robot Controller app to match the hue detected by the RGB sensor.
      relativeLayoutId = hardwareMap.appContext.getResources().getIdentifier("RelativeLayout", "id", hardwareMap.appContext.getPackageName());
      relativeLayout = ((Activity) hardwareMap.appContext).findViewById(relativeLayoutId);
    }

    private void gyroLoop() {   
      telemetry.addData("IMU", imu.isGyroCalibrated() ? "Initialized" : "Initializing...");
      angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
      telemetry.addData("ANGLE", angles);
      telemetry.addData("HEADING", angles.firstAngle);
    }

    // It is possible for buttons to be read multiple times through a loop, triggering
    // actions multiple times unintentionally.  Using a debounce routine fixes this issue
    // by ignoring further presses of a button until it has been released.
    //
    // Pattern:
    // Use a boolean variable to track whether a button has been released yet.
    // boolean x_released = true; // Button is not currently pressed
    // if (gamepad1.x) {  // X Button is pressed
    //     if (x_released) { if the x button is not currently pressed...
    //         x_released = false; // mark the button as not having been released yet
    //         // Do an action here
    //     }
    // }
    // else {
    //     x_released = true;

    private void setManualMode() {
      //telemetry.addData("MANUAL MODE:", manualControl); // <-- See above comment block for explanation
      if(gamepad1.right_bumper) {
        if(!rightBumperDown){
          rightBumperDown = true;
          manualControl = !manualControl; // TODO NO TOGGLES ALLOWED THIS IS HORRIBLE
        }
      }
      else {
        rightBumperDown = false;
      }
    }

    private void dropBlock() {
      if(gamepad1.left_bumper) {
        if(!leftBumperDown){
          leftBumperDown = true;
          hand.setPosition(0.3);
        }
      } else {
        leftBumperDown = false;
      }
    }

    private void resetArm() {
      if(gamepad1.right_stick_button) {
        if(!rightStickClick){
          arcadeMode = !arcadeMode;
        }
      } else {
        rightStickClick = false;
      }
    }

    private void manualMoveArm() {
      manualArmPower = (gamepad1.left_trigger * -1) + (gamepad1.right_trigger);
      manArm.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
      manArm.setPower(manualArmPower);
    }

    private void selectPosition() {
      if(gamepad1.a){
        armState = "0block";
      }
      else if (gamepad1.x){
        armState = "1block";
      }
      else if (gamepad1.y){
        armState = "2block";
      }
      else if (gamepad1.b){
        armState = "3block";
      }
      else if (gamepad1.dpad_up){
        armState = "capstone";
      }
    }

    private void moveArm() {
      //telemetry.addData("ManArm", manArm.getCurrentPosition());
      if(manualControl){
        manualMoveArm(); // <-- If manual control mode is active...
                          // ...revert to trigger control.
                          // RightTrigger = up
                          // LeftTrigger = down
        return;          // <-- This will exit the function early so...
                          // ...the state machine doesn't control the arm
      }
      switch(armState){
        case "0block":
        manArm.setTargetPosition(0);
        if(frontDistance.getDistance(DistanceUnit.CM) < 5.5 && !leftBumperDown) {
          hand.setPosition(1);
          if(hand.getPosition() == 1) {
            armState = "1block";
          }
        }
        break;

        case "1block":
        manArm.setTargetPosition(420);
        if(frontDistance.getDistance(DistanceUnit.CM) < 5.5 && !leftBumperDown) {
          hand.setPosition(1);
        }

        break;

        case "2block":
        manArm.setTargetPosition(565);
        if(frontDistance.getDistance(DistanceUnit.CM) < 5.5 && !leftBumperDown) {
          hand.setPosition(1);
        }

        break;

        case "3block":
        manArm.setTargetPosition(775);
        if(frontDistance.getDistance(DistanceUnit.CM) < 5.5 && !leftBumperDown) {
          hand.setPosition(1);
        }

        break;

        case "capstone":
        manArm.setTargetPosition(1100);
        if(frontDistance.getDistance(DistanceUnit.CM) < 5.5 && !leftBumperDown) {
          hand.setPosition(1);
        }

        break;
      }

      manArm.setPower(0.3);
      manArm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
      telemetry.addData("ArcadeMode:", arcadeMode);
    }

    private void moveClaw() {
      hand.setPosition(0.3);
    }

    private void foundGrab() {
      if (gamepad1.dpad_left){
        leftFound.setPosition(0.4);
        rightFound.setPosition(0.5);
      } else {
        leftFound.setPosition(1);
        rightFound.setPosition(0);
      }
    }
}
