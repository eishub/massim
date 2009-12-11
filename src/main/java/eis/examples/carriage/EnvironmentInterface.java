package eis.examples.carriage;

import java.util.LinkedList;

import eis.*;
import eis.exceptions.EntityException;
import eis.exceptions.EnvironmentInterfaceException;
import eis.exceptions.ManagementException;
import eis.iilang.Percept;
import eis.iilang.Numeral;
import eis.iilang.EnvironmentCommand;

public class EnvironmentInterface extends EIDefaultImpl implements Runnable {
	
	private Environment env;
	
	public EnvironmentInterface() {
		
		env = new Environment();
		
		try {

			this.addEntity("robot1");
			this.addEntity("robot2");

		} catch (EntityException e) {
			e.printStackTrace();
		}
		
		Thread t = new Thread( this ); 
		//t.setPriority(Thread.MIN_PRIORITY);
		t.start();
		
	}
	
	@Override
	public void manageEnvironment(EnvironmentCommand command)
			throws ManagementException {

		throw new ManagementException("No environment-commands supported.");
		
	}

	public void run() {

		while(true) {

			// notify about free entities
			for( String free : this.getFreeEntities() )
				this.notifyFreeEntity(free);
						
			// tell current step
			long step = env.getStepNumber();
			Percept p = new Percept("step", new Numeral(step) );
			for( String entity : this.getEntities() )
				try {
					this.notifyAgentsViaEntity(p, entity);
				} catch (EnvironmentInterfaceException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			
			
			// block for 1 second
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println(e);

			}
			
		}
		
	}

	@Override
	public LinkedList<Percept> getAllPerceptsFromEntity(String entity) {

		LinkedList<Percept> ret = new LinkedList<Percept>();
		
		if( entity.equals("robot1"))
			ret.add(
					new Percept(
							"carriagePos", 
							new Numeral(env.getRobotPercepts1())
							)
					);
		else if( entity.equals("robot2"))
			ret.add(
					new Percept(
							"carriagePos", 
							new Numeral(env.getRobotPercepts2())
							)
					);

		return ret;
	}
	
	public Percept actionpush(String entity) {
		
		// push
		if( entity.equals("robot1") )
			env.robotPush1();
		if( entity.equals("robot2") )
			env.robotPush2();

		return new Percept("success");

	}

	public Percept actionwait(String entity) {
		
		// push
		if( entity.equals("robot1") )
			env.robotWait1();
		if( entity.equals("robot2") )
			env.robotWait2();

		return new Percept("success");

	}

	@Override
	public void release() {

		env.release();
		
		env = null;
		
	}

	@Override
	public boolean isConnected() {

		return true;
	
	}

	@Override
	public String requiredVersion() {
		return "0.2rc1";
	}

}
