package frc.robot.subsystems;
import com.ctre.phoenix6.configs.Pigeon2Configuration;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.PathPlannerLogging;
import com.pathplanner.lib.util.ReplanningConfig;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.SwerveConstants;
import frc.robot.Constants.SwerveModuleConstants;

import java.util.Optional;


public class SwerveSubsystem extends SubsystemBase{
  // Modules
  private final SwerveModule leftFrontModule, rightFrontModule, leftRearModule, rightRearModule;
  // Gyro
  private final Pigeon2 gyro = new Pigeon2(SwerveConstants.gyroID);
  private final Pigeon2Configuration gyroConfig = new Pigeon2Configuration();
  // Odometer
  private SwerveDriveOdometry m_odometer;
  // Field
  private Field2d field = new Field2d();

  public SwerveSubsystem(){
    /* Gyro */
    gyroConfig.MountPose.MountPoseYaw = -10;
    gyro.getConfigurator().apply(gyroConfig);
    resetGyro();
    /* Modules */
    leftFrontModule = new SwerveModule(
      SwerveConstants.leftFrontDriveID, 
      SwerveConstants.leftFrontTurningID, 
      SwerveConstants.leftFrontdriveMotorReversed, 
      SwerveConstants.leftFrontTurningMotorReversed, 
      SwerveConstants.leftFrontCANCoderID, 
      SwerveConstants.leftFrontOffset);
    rightFrontModule = new SwerveModule(
      SwerveConstants.rightFrontDriveID,
      SwerveConstants.rightFrontTurningID,
      SwerveConstants.rightFrontDriveMotorReversed, 
      SwerveConstants.rightfrontTurningMotorReversed, 
      SwerveConstants.rightFrontCANCoderID, 
      SwerveConstants.rightFrontOffset);
    leftRearModule = new SwerveModule(
      SwerveConstants.leftRearDriveID, 
      SwerveConstants.leftRearTurningID, 
      SwerveConstants.leftRearDriveMotorreversed, 
      SwerveConstants.leftRearTurningMotorReversed, 
      SwerveConstants.leftRearCANCoderID, 
      SwerveConstants.leftRearOffset);
    rightRearModule = new SwerveModule(
      SwerveConstants.rightRearDriveID, 
      SwerveConstants.rightRearTurningID, 
      SwerveConstants.rightRearDriveMotorReversed, 
      SwerveConstants.rightRearTurningMotorReversed, 
      SwerveConstants.rightRearCANCoderID, 
      SwerveConstants.rightRearOffset);

    /* Odometer */
    m_odometer = new SwerveDriveOdometry(
      SwerveConstants.swerveKinematics, 
      gyro.getRotation2d(), 
      getModulePosition());

    /* Auto builder */
    AutoBuilder.configureHolonomic(
      this::getOdometer, 
      this::setOdometer, 
      this::getSpeeds, 
      this::driveFieldRelative,
      new HolonomicPathFollowerConfig(
          new PIDConstants(4, 0, 0), // Translation constants 
          new PIDConstants(2, 0, 0.002), // Rotation constants 
          SwerveModuleConstants.maxDriveMotorSpeed, 
          Units.inchesToMeters(14.32), // Drive base radius (distance from center to furthest module) 
          new ReplanningConfig()
      ),
      () -> {
        // Boolean supplier that controls when the path will be mirrored for the red alliance
        // This will flip the path being followed to the red side of the field.
        // THE ORIGIN WILL REMAIN ON THE BLUE SIDE
        Optional<Alliance> alliance = DriverStation.getAlliance();
        if (alliance.isPresent())
         return alliance.get() == DriverStation.Alliance.Red;
        return false;
      },
      this
    );

    // Set up custom logging to add the current path to a field 2d widget
    PathPlannerLogging.setLogActivePathCallback((poses) -> field.getObject("path").setPoses(poses));
    SmartDashboard.putData("Field", field);
  }


  public void resetGyro(){
    gyro.reset();
  }
  public ChassisSpeeds getSpeeds() {
    return SwerveConstants.swerveKinematics.toChassisSpeeds(getModuleStates());
  }
  public SwerveModulePosition[] getModulePosition(){
    return new SwerveModulePosition[]{
      leftFrontModule.getPosition(),
      rightFrontModule.getPosition(),
      leftRearModule.getPosition(),
      rightRearModule.getPosition()
    };
  }
  public SwerveModuleState[] getModuleStates(){
    return new SwerveModuleState[]{
      leftFrontModule.getState(),
      rightFrontModule.getState(),
      leftRearModule.getState(),
      rightRearModule.getState()
    };
  }
  public void setModuleStates(SwerveModuleState[] desiredStates){
    SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, SwerveModuleConstants.maxDriveMotorSpeed);
    leftFrontModule.setDesiredState(desiredStates[0]);
    rightFrontModule.setDesiredState(desiredStates[1]);
    leftRearModule.setDesiredState(desiredStates[2]);
    rightRearModule.setDesiredState(desiredStates[3]);
  }
  public void stopModules(){
    leftFrontModule.stopMotors();
    leftRearModule.stopMotors();
    rightFrontModule.stopMotors();
    rightRearModule.stopMotors();
  }
  // Manual Drive
  public void drive(double xSpeed, double ySpeed, double zSpeed, boolean fieldOriented){
    SwerveModuleState[] states = null;
    if(fieldOriented){
      states = SwerveConstants.swerveKinematics.toSwerveModuleStates(
        ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, zSpeed, gyro.getRotation2d()));
    }else{
      states = SwerveConstants.swerveKinematics.toSwerveModuleStates(new ChassisSpeeds(xSpeed, ySpeed, zSpeed));
    }
    setModuleStates(states);
  }
  // Auto Drive
  public void driveFieldRelative(ChassisSpeeds fieldRelativeSpeeds){
    ChassisSpeeds transformed = ChassisSpeeds.fromFieldRelativeSpeeds(fieldRelativeSpeeds, getOdometer().getRotation());
    ChassisSpeeds targetSpeeds = ChassisSpeeds.discretize(transformed, 0.02);
    SwerveModuleState[] states = SwerveConstants.swerveKinematics.toSwerveModuleStates(targetSpeeds);
    setModuleStates(states);
  }

  public Pose2d getOdometer(){
    return m_odometer.getPoseMeters();
  }
  // Set Odometer Pose
  public void setOdometer(Pose2d pose){
    m_odometer.resetPosition(gyro.getRotation2d(), getModulePosition(), pose);
  }

  @Override
  public void periodic(){
    m_odometer.update(gyro.getRotation2d(), getModulePosition());
    field.setRobotPose(m_odometer.getPoseMeters());
    // SmartDashboard.putNumber("X", m_odometer.getPoseMeters().getX());
    // SmartDashboard.putNumber("Y", m_odometer.getPoseMeters().getY());
    SmartDashboard.putNumber("LF_angle", leftFrontModule.getTurningPosition());
    SmartDashboard.putNumber("LR_angle", leftRearModule.getTurningPosition());
    SmartDashboard.putNumber("LF_Position", leftFrontModule.getDrivePosition());
    SmartDashboard.putNumber("LR_Position", leftRearModule.getDrivePosition());
    SmartDashboard.putNumber("RF_angle", rightFrontModule.getTurningPosition());
    SmartDashboard.putNumber("RR_angle", rightRearModule.getTurningPosition());
    SmartDashboard.putNumber("RF_Position", rightFrontModule.getDrivePosition());
    SmartDashboard.putNumber("RR_Position", rightRearModule.getDrivePosition());
    // SmartDashboard.putNumber("robotAngle", gyro.getRotation2d().getDegrees());
  }
  
}