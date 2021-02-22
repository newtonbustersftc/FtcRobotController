package org.firstinspires.ftc.teamcode;

import android.content.SharedPreferences;
import android.util.Log;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.io.File;

@TeleOp(name="DriverOpMode Test", group="Test")
//@Disabled
public class DriverOpModeTest extends OpMode {
    RobotHardware robotHardware;
    boolean fieldMode;
    RobotProfile robotProfile;

    Pose2d currPose;
    double fieldHeadingOffset;
    boolean dpadLeftDown = false;
    boolean dpadRightDown = false;
    double shootServoPos = 0.6;
    int ledNum = 0;
    boolean yPressed = false;
    boolean aPressed = false;
    boolean bPressed = false;

    @Override
    public void init() {
        try{
            robotProfile = RobotProfile.loadFromFile(new File("/sdcard/FIRST/profile.json"));
        } catch (Exception e) {
        }

        fieldMode = true;
        Logger.init();
        robotHardware = new RobotHardware();
        robotHardware.init(hardwareMap, robotProfile);
        robotHardware.setShooterPosition(true);
        SharedPreferences prefs = AutonomousOptions.getSharedPrefs(hardwareMap);
    }

    @Override
    public void loop() {
        robotHardware.getBulkData1();
        robotHardware.getBulkData2();
        robotHardware.getTrackingWheelLocalizer().update();
        currPose = robotHardware.getTrackingWheelLocalizer().getPoseEstimate();

        handleMovement();
        // field mode or not
        if (gamepad1.left_trigger > 0) {
            fieldMode = true;
            fieldHeadingOffset = currPose.getHeading();
        } else if (gamepad1.right_trigger > 0) {
            fieldMode = false;  //good luck driving
        }
        // test controls
        if (gamepad1.dpad_right && !dpadRightDown) {
            robotHardware.setArmNextPosition();
            dpadRightDown = true;
        }
        else {
            dpadRightDown = gamepad1.dpad_right;
        }
        if (gamepad1.dpad_left && !dpadLeftDown) {
            robotHardware.setArmPrevPosition();
            dpadLeftDown = true;
        }
        else {
            dpadLeftDown = gamepad1.dpad_left;
        }

        //robotHardware.setShooterPosition(gamepad1.x);

        robotHardware.setLed1(ledNum==0);
        robotHardware.setLed2(ledNum==1);
        robotHardware.setLed3(ledNum==2);
        if (gamepad1.y && !yPressed) {
            ledNum = (ledNum+1) % 3;
        }
        yPressed = gamepad1.y;
        if (gamepad1.a && !aPressed) {
            shootServoPos -= 0.02;
            robotHardware.shootServo.setPosition(shootServoPos);
        }
        aPressed = gamepad1.a;
        if (gamepad1.b && !bPressed) {
            shootServoPos += 0.02;
            robotHardware.shootServo.setPosition(shootServoPos);
        }
        bPressed = gamepad1.b;
        if (gamepad1.dpad_up) {
            robotHardware.startShootMotor();
            robotHardware.ringHolderUp();
        }
        if (gamepad1.dpad_down) {
            robotHardware.stopShootMotor();
            robotHardware.ringHolderDown();
        }
        telemetry.addData("LeftE", robotHardware.getEncoderCounts(RobotHardware.EncoderType.LEFT));
        telemetry.addData("RightE", robotHardware.getEncoderCounts(RobotHardware.EncoderType.RIGHT));
        telemetry.addData("HorizE", robotHardware.getEncoderCounts(RobotHardware.EncoderType.HORIZONTAL));
        telemetry.addData("Pose:", robotHardware.getTrackingWheelLocalizer().getPoseEstimate());
        telemetry.addData("Shoot Servo:", shootServoPos);
        telemetry.addData("LEDNum:", ledNum);
        telemetry.addData("ArmPos", robotHardware.getEncoderCounts(RobotHardware.EncoderType.ARM));
   }

    @Override
    public void stop() {
        // open the clamp to relief the grabber servo
        try {
            robotHardware.stopAll();
            Logger.logFile("DriverOpMode Test stop() called");
            Logger.flushToFile();
        } catch (Exception e) {
            Log.e("DriverOpMode", Log.getStackTraceString(e));
        }
    }
   private void handleMovement() {
        double turn = gamepad1.right_stick_x / 2;
        double power = Math.hypot(gamepad1.left_stick_x, -gamepad1.left_stick_y);
        double moveAngle = Math.atan2(-gamepad1.left_stick_y, gamepad1.left_stick_x) - Math.PI / 4.5;

        if (fieldMode) {
            moveAngle += currPose.getHeading() - fieldHeadingOffset - Math.PI / 2;
        }

        if (gamepad1.left_bumper) {
            power = power / 3.5;
            turn = turn / 13;
        }
        robotHardware.mecanumDriveTest(power, moveAngle, turn, 0);
    }
}