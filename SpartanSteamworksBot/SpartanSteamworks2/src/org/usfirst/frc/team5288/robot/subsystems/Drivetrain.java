package org.usfirst.frc.team5288.robot.subsystems;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.PIDController;
import org.usfirst.frc.team5288.robot.Robot;
import org.usfirst.frc.team5288.robot.RobotMap;
import org.usfirst.frc.team5288.robot.commands.DriveCommands.ManualDrive;

import edu.wpi.first.wpilibj.Encoder;
/**
 *
 */
public class Drivetrain extends Subsystem {
	public final double encoderAccuracy = 0.00063837162; // 10th of a mm
	/*
	 * Drive Kinematics math
	 * a = (v1-v2)/2
	 * Distance = Velocity1*time + 1/2*Acceleration*time^2 
	 */
	//**DRIVETRAIN CONSTANTS**
	public final double wheelRadius = 0.0508; //meters
	public final double wheelcirc = 2*Math.PI*wheelRadius; 
	public final double topSpeed =  3.048; // meters per second
	public final double vfeedForward = 1/3.048; //meters per second
	public final double kp = 0.04;
	private double encPPR = 2048;
	//*******************MOTOR CONTROLLER OBJECTS**************
	//These Motor controller objects will always be synced in pairs of output.
	private VictorSP lmotor1 = new VictorSP(RobotMap.LDriveMotor1);//Left Gearbox Motor #
	private VictorSP lmotor2 = new VictorSP(RobotMap.LDriveMotor2);//Left Gearbox Motor #
	private VictorSP rmotor1 = new VictorSP(RobotMap.RDriveMotor1);//Right Gearbox Motor #1
	private VictorSP rmotor2 = new VictorSP(RobotMap.RDriveMotor2);//Right Gearbox Motor #2
	private boolean isBrakeMode = true;
	//**Drive Variables**
	private double throttle = 1;
	private double lPower = 0;//Raw Power percentage being output to the left gearbox.
	private double rPower = 0;//Raw Power percentage being output to the right gearbox.
	
	//**ENCODER VARIABLES**
	private Encoder rEncoder ;
	private Encoder lEncoder ;
	
	
	//**SPEED CALCULATION BASED VARIABLES**	//Encoder Tracking variables
		//Left
	private double lastSpeedL = 0;
	private double lastAccelL = 0;
	private double currentAccelL = 0;
	private double jerkL = 0;
	private double currentSpeedL = 0;
	private double targetAccelL = 0;
	private double targetSpeedL = 0;
	private double encLastL = 0;
	private double encCurrentL = 0;
	private double encDiffL = 0;
		//Right
	private double lastAccelR = 0;
	private double targetAccelR = 0;
	private double currentAccelR = 0;
	private double jerkR = 0;
	private double lastSpeedR = 0;
	private double currentSpeedR = 0;
	private double targetSpeedR = 0;
	private double encLastR = 0;
	private double encCurrentR = 0;
	private double encDiffR = 0;
		//Time tracking
	private double timeLast = 0;
	private double timeCurrent = 0;
	private double timeDiff = 0;
	//*Built-In State Machine*
	//The state machine ensures that the drivetrain either A: Runs it's P.I.D controls,
	// or B: runs off of the raw data being supplied to it, one of the two will always occur due to the default command.
	public enum drivestates  {SetVelocity,MANUAL, AUTOPID};
	private drivestates currentState = drivestates.MANUAL;
	
	//VELOCITY PID variables
	//Left
	private double vProportionalL = 0;
	private double vFeedForwardL = 0;
	private double vDerivativeL = 0;
	private double vIntegralL = 0;
	//Right
	private double vProportionalR = 0;
	private double vFeedForwardR = 0;
	private double vDerivativeR = 0;
	private double vIntegralR = 0;
	//VELOCITY PID CONSTANTS
	public final double vProportionalGainL = 0.04;
	public final double vDerivativeGainL = 0;
	public final double vIntegralGainL = 0;
	public final double vProportionalGainR = 0.04;
	public final double vDerivativeGainR = 0;
	public final double vIntegralGainR = 0;
	//Acceleration PID CONSTANTS
	private double aProportionalGainL = 0;
	private double aDerivativeGainL = 0;
	private double aIntegralGainL = 0;
	private double aProportionalGainR = 0;
	private double aDerivativeGainR = 0;
	private double aIntegralGainR = 0;
	//******************************Instantiates the DRIVETRAIN SUBCLASS***************************
	public Drivetrain()
	{
		rEncoder = new Encoder(RobotMap.RDriveEncoder1,RobotMap.RDriveEncoder2,RobotMap.RDriveEncoderIndex,false);	
		lEncoder = new Encoder(RobotMap.LDriveEncoder1,RobotMap.LDriveEncoder2,RobotMap.LDriveEncoderIndex,false);	
		rEncoder.setMaxPeriod(1);
		lEncoder.setMaxPeriod(1);
		rEncoder.setSamplesToAverage(10);
		lEncoder.setSamplesToAverage(10);		
		rEncoder.setDistancePerPulse(1);
		lEncoder.setDistancePerPulse(1);

	}
	
	//******************************DriveTrain Methods and Procedures******************************  

	public void initDefaultCommand() {
		setDefaultCommand(new ManualDrive());
	}
	public void update()
	{
		updateSensorVals();
		updateOutputs();
		updateSmartDashboard();
	}
	public void updateSensorVals(){
		System.out.println("LeftDistance: "+getLeftDistanceMeters());
		System.out.println("RightDistance: " + getRightDistanceMeters());
		//Load last Values
		lastSpeedL = currentSpeedL;
		lastSpeedR = currentSpeedR;
		lastAccelL = currentAccelL;
		lastAccelR = currentAccelR;
		encLastR = encCurrentR;
		encLastL = encCurrentL;
		timeLast = timeCurrent;

		//Update Current Values
		timeCurrent = System.currentTimeMillis();
		encCurrentL = getLeftDistanceMeters();
		encCurrentR = getRightDistanceMeters();
		System.out.println("encCurrentL:" + lEncoder.getDistance());
		System.out.println("encCurrentR:" +	rEncoder.getDistance());

		//******Calculate New Values*******
		timeDiff = timeCurrent - timeLast;//Calculate time difference
		encDiffL = encCurrentL - encLastL;//Calculate encoder difference
		encDiffR = encCurrentR - encLastR;//Calculate encoder difference
		//Calculate The Current speed
		currentSpeedL = encDiffL/timeDiff;
		currentSpeedR = encDiffR/timeDiff;
		//******Calculate Acceleration and difference in acceleration******
		currentAccelL = (lastSpeedL - currentSpeedL)/timeDiff;
		currentAccelR = (lastSpeedR- currentSpeedR)/timeDiff;
		jerkL = (currentAccelL - lastAccelL)/timeDiff;
		jerkR = (currentAccelR - lastAccelR)/timeDiff;

	}
	private void updateOutputs(){
		switch(currentState)
		{
		case SetVelocity:
			double outputL,outputR;
			vProportionalL = (targetSpeedL - currentSpeedL); 
			vProportionalR = (targetSpeedR - currentSpeedR); 
			vIntegralL += vProportionalL;
			vIntegralR += vProportionalR;
			if( vIntegralL > 1)
			{
				vIntegralL = 1;
			}
			if( vIntegralR > 1)
			{
				vIntegralR = 1;
			}
			vDerivativeL = (currentAccelL);
			vDerivativeR = (currentAccelR);
			outputL = targetSpeedL*vFeedForwardL + vProportionalGainL*vProportionalL + 
					vIntegralL*vIntegralGainL - vDerivativeGainL*vDerivativeL; //P+I-D = output
			outputR = targetSpeedR*vFeedForwardR + vProportionalGainR*vProportionalR + 
					vIntegralR*vIntegralGainR - vDerivativeGainR*vDerivativeR; //P+I-D = output
			
			outputToMotors(outputL,outputR);
			break;
		case MANUAL:
			outputToMotors(lPower,rPower); 
			break;
		case AUTOPID:
			//setacceleration?
			vProportionalL = (targetAccelL - currentAccelL); 
			vProportionalR = (targetAccelR - currentAccelR); 
			vIntegralL += vProportionalL;
			vIntegralR += vProportionalR;
			if( vIntegralL > 1)
			{
				vIntegralL = 1;
			}
			if( vIntegralR > 1)
			{
				
				vIntegralR = 1;
			}
			vDerivativeL = (jerkL);
			vDerivativeR = (jerkR);
			outputL = jerkL*vFeedForwardL + aProportionalGainL*vProportionalL + 
					vIntegralL*aIntegralGainL - aDerivativeGainL*vDerivativeL; //P+I-D = output
			outputR = jerkR*vFeedForwardR + aProportionalGainR*vProportionalR + 
					vIntegralR*aIntegralGainR - aDerivativeGainR*vDerivativeR; //P+I-D = output
			
			outputToMotors(outputL,outputR);
			break;
			
		}
	}
	private void setTargetSpeeds(double Left, double Right)//Sets the PID to control the robot.
	{
		currentState = drivestates.SetVelocity;
		targetSpeedL = Left;
		targetSpeedR = Right;
	}
	//**Throttle is used to maximize the output potential by the MANUAL driver
	//TODO: Implement Throttle
	private void outputToMotors(double pwrLeft, double pwrRight){
		lmotor1.set(-pwrLeft);
		lmotor2.set(-pwrLeft);
		rmotor1.set(pwrRight);
		rmotor2.set(pwrRight);
	}
	public void setThrottle(double newThrottle)
	{
		throttle = newThrottle;
	}
	public double getThrottle(){
		return throttle;
	}
	public void setLPower(double power)
	{
		currentState = drivestates.MANUAL;
		lPower = power;
	}
	public void setRPower(double power)
	{
		currentState = drivestates.MANUAL;
		rPower = power;
	}
	public double getLeftDistanceMeters()
	{
		return lEncoder.get()*(wheelcirc/encPPR);
	}
	public double getRightDistanceMeters()
	{
		return rEncoder.get()*(wheelcirc/encPPR);
	}
	public void resetEncoders()//Should not need to be used except when changing control periods.
	{
		lEncoder.reset();
		rEncoder.reset();
	}

    private double rotationsTometers(double rotations) {
        return rotations * (wheelRadius * Math.PI);
    }
    private double rpmTometersPerSecond(double rpm) {
        return rotationsTometers(rpm) / 60;
    }
    private double metersToRotations(double meters) {
        return meters / (2* wheelRadius * Math.PI);
    }
    private double metersPerSecondToRpm(double meters_per_second) {
        return metersToRotations(meters_per_second) * 60;
    }
    private void updateSmartDashboard()
    {
    	/*
    	SmartDashBoard.putNumber("leftTargetSpeed", targetSpeedL);
    	SmartDashBoard.putNumber("rightTargetSpeed", targetSpeedR);
    	Robot.table.putNumber("leftDrivePower",lPower);
    	Robot.table.putNumber("rightDrivePower",rPower);
    	Robot.table.putNumber("leftDriveAccel",currentAccelL);
    	Robot.table.putNumber("rightDriveAccel",currentAccelR);
    	Robot.table.putNumber("leftDriveSpeed",currentSpeedL);
    	Robot.table.putNumber("rightDriveSpeed",currentSpeedL);
    	Robot.table.putNumber("leftDriveJerk",jerkL);
    	Robot.table.putNumber("rightDriveJerk",jerkR);
    	Robot.table.putNumber("LeftEncoderDistance",getLeftDistanceMeters() );
    	Robot.table.putNumber("RightEncoderDistance",getLeftDistanceMeters() );
    	*/
    }
 }

