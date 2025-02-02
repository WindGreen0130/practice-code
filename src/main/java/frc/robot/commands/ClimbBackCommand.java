// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.ClimbSubsystem;

public class ClimbBackCommand extends Command {
  private final ClimbSubsystem m_climbSubsystem;
  /** Creates a new ClimbBackCommand. */
  public ClimbBackCommand(ClimbSubsystem climbSubsystem) {
    // Use addRequirements() here to declare subsystem dependencies.
    this.m_climbSubsystem = climbSubsystem;
    addRequirements(m_climbSubsystem);
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {}

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    m_climbSubsystem.rightClimbBack();
    m_climbSubsystem.leftClimbBack();
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    m_climbSubsystem.StopMotors();
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return false;
  }
}
