package it.polito.dp2.NFFG.sol2;

import it.polito.dp2.NFFG.lab2.ReachabilityTester;
import it.polito.dp2.NFFG.lab2.ReachabilityTesterException;
import it.polito.dp2.NFFG.lab2.ServiceException;
import it.polito.dp2.NFFG.lab2.UnknownNameException;

public class ReachabilityTesterFactory extends it.polito.dp2.NFFG.lab2.ReachabilityTesterFactory{

	private ReachabilityTester tester;
	@Override
	public ReachabilityTester newReachabilityTester() throws ReachabilityTesterException {
		try{
			tester= new it.polito.dp2.NFFG.sol2.ReachabilityTester();
		} catch(Exception e){
			e.printStackTrace();
		}
		return tester;
	}

}
