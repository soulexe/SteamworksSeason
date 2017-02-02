package org.usfirst.frc.team5288.robot.subsystems;

import edu.wpi.first.wpilibj.command.Subsystem;

/**
 *
 */
public class Intake extends Subsystem {

    // Put methods for controlling this subsystem
    // here. Call these from Commands.
	private double currentPower = 0;
	private double feedforward = 0.6;// (Robot velocity)
	/* This subsystem only needs three commands, A standby command that forces intake, A command that forces no intake.
	 * Therefore, the subsystem only tneeds to track the current power. 
	 * 
	*/
    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand (new intakeStandbyCommand());
    }
    public void setPower(double power)
    {
    	currentPower = power;
    }
    public void update()
    {
    	updateValues();
    	updateOutputs();
    }
    public void updateValues(){
		
	}
	public void updateOutputs(){
		
	}


    
}

