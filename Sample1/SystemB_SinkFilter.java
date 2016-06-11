import java.util.*;						// This class is used to interpret time words


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;		// This class is used to format and write time in a string format.

public class SystemB_SinkFilter extends FilterFramework {
	
	public void run()
    {
		/************************************************************************************
		*	TimeStamp is used to compute time using java.util's Calendar class.
		* 	TimeStampFormat is used to format the time value so that it can be easily printed
		*	to the terminal.
		*************************************************************************************/

		Calendar TimeStamp = Calendar.getInstance();
		SimpleDateFormat TimeStampFormat = new SimpleDateFormat("yyyy:dd:hh:mm:ss");
		PrintWriter writer;
		try {
			writer = new PrintWriter("/Users/d065820/Documents/HU_Belrin/MMS/MMSE-Ex-Sheet-02-sources/Output/Aufgabe1B2.txt", "UTF-8");

		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream

		byte databyte = 0;				// This is the data byte read from the stream
		int bytesread = 0;				// This is the number of bytes read from the stream

		long measurement=0;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		int i;							// This is a loop counter
		
		int count_outliers=0;
		double altitude_cash=0.0;
		
		long currentTime=0;
		double cash_Pressure=0.0;
		List<ArrayList<Object>> WaitingLIst=new ArrayList<ArrayList<Object>>();
		ArrayList<Object> dataset= new ArrayList<Object>();
		
		ArrayList<Object> dataset_right= new ArrayList<Object>();
		
		/*************************************************************
		*	First we announce to the world that we are alive...
		**************************************************************/

		System.out.print( "\n" + this.getName() + "::Sink Reading "); 
		System.out.print("\n");
		writer.format("%1s%40s%20s%20s","Time:", "Temperature(C):", "Altitude(m):", "Pressure(psi):");
		writer.print("\n______________________________________________________________________________________");
		//System.out.print( "\n" + "Time:"+"              "+"Tenpreture(C)"+ "    "+"Altitude(m)"+"    "+"Pressure (psi)");
		while (true)
		{
			try
			{
				/***************************************************************************
				// We know that the first data coming to this filter is going to be an ID and
				// that it is IdLength long. So we first decommutate the ID bytes.
				****************************************************************************/

				id = 0;

				for (i=0; i<IdLength; i++ )
				{
					databyte = ReadFilterInputPort();	// This is where we read the byte from the stream...

					id = id | (databyte & 0xFF);		// We append the byte on to ID...

					if (i != IdLength-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id = id << 8;					// to make room for the next byte we append to the ID

					} // if

					bytesread++;						// Increment the byte count

				} // for

				/****************************************************************************
				// Here we read measurements. All measurement data is read as a stream of bytes
				// and stored as a long value. This permits us to do bitwise manipulation that
				// is neccesary to convert the byte stream into data words. Note that bitwise
				// manipulation is not permitted on any kind of floating point types in Java.
				// If the id = 0 then this is a time value and is therefore a long value - no
				// problem. However, if the id is something other than 0, then the bits in the
				// long value is really of type double and we need to convert the value using
				// Double.longBitsToDouble(long val) to do the conversion which is illustrated.
				// below.
				*****************************************************************************/

				measurement = 0;

				for (i=0; i<MeasurementLength; i++ )
				{
					databyte = ReadFilterInputPort();
					
					measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...

					if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement = measurement << 8;				// to make room for the next byte we append to the
																	// measurement
					} // if

					bytesread++;									// Increment the byte count

				} // if

				/****************************************************************************
				// Here we look for an ID of 0 which indicates this is a time measurement.
				// Every frame begins with an ID of 0, followed by a time stamp which correlates
				// to the time that each proceeding measurement was recorded. Time is stored
				// in milliseconds since Epoch. This allows us to use Java's calendar class to
				// retrieve time and also use text format classes to format the output into
				// a form humans can read. So this provides great flexibility in terms of
				// dealing with time arithmetically or for string display purposes. This is
				// illustrated below.
				****************************************************************************/
                
                
				if ( id == 0 )
				{
					if (dataset_right.size()>0){
						writer.print("\n");
						if (dataset_right.get(0)==":"){
							writer.format("%1s%30s%20s%20s",dataset_right.get(0), dataset_right.get(3), dataset_right.get(1), dataset_right.get(2));
						}else{
							writer.format("%1s%20s%20s%20s",dataset_right.get(0), dataset_right.get(3), dataset_right.get(1), dataset_right.get(2));	
						}
						dataset_right.clear();
					}
					TimeStamp.setTimeInMillis(measurement);
					currentTime=measurement;
					dataset=new ArrayList<Object>();
					//dataset_right.add(measurement);
					
					

				} // if

				/****************************************************************************
				// Here we pick up a measurement (ID = 3 in this case), but you can pick up
				// any measurement you want to. All measurements in the stream are
				// decommutated by this class. Note that all data measurements are double types
				// This illustrates how to convert the bits read from the stream into a double
				// type. Its pretty simple using Double.longBitsToDouble(long value). So here
				// we print the time stamp and the data associated with the ID we are interested
				// in.
				****************************************************************************/
				if ( id == 2 )
				{
					Double erg_id2=Double.longBitsToDouble(measurement)/3.28084;
					altitude_cash=erg_id2;
					
				} // if
				
				
				if ( id == 3 )
				{ 
					Double erg_id3=Double.longBitsToDouble(measurement);

					if ((erg_id3<0.0 || Math.abs(cash_Pressure-erg_id3)>10.0) &&cash_Pressure>0.0){  
					
						if (cash_Pressure==0.0){
							//beim ersten
							//System.out.println("HAHA");
							cash_Pressure=erg_id3;				
						}
						
						
						
						dataset.add(TimeStampFormat.format(TimeStamp.getTime()));
						dataset.add(altitude_cash);
						dataset.add(erg_id3);
						
						dataset_right.add(":");
						dataset_right.add(":");
						dataset_right.add(":");
						count_outliers++;
					
					} else{
						if (count_outliers>0){
								 for (int ii = WaitingLIst.size()-count_outliers; ii < WaitingLIst.size(); ii++) {
										 //System.out.println( " "+ WaitingLIst.get(ii).get(2));
										 WaitingLIst.get(ii).set(3,(long) ((measurement+cash_Pressure)/2.0));
										 count_outliers=0;
										 
										 
								 } 
						}
							  //System.out.println(TimeStamp.getTime() + " ID = " + id + " " + erg_id3);
						
						dataset_right.add(TimeStampFormat.format(TimeStamp.getTime()));
						dataset_right.add(altitude_cash);
						dataset_right.add(erg_id3);
						   	  //writer.println(TimeStamp.getTime() + " ID = " + id + " " +erg_id3);
						cash_Pressure=erg_id3;
							  //System.out.print("Cash "+cash_Pressure);
							 
						}
					}
				
				
				
				if ( id == 4 )
				{
					Double erg =  new Double((Double.longBitsToDouble(measurement)-32)/1.8);	
					if (dataset.size()>0){
						//System.out.println("blabla");
						dataset.add(erg);
						dataset_right.add(":");
						WaitingLIst.add(dataset);
						//System.out.println(WaitingLIst.get(0).size());  
						
					}else{
						dataset_right.add(erg);
					}
				
				} // if

				//System.out.print( "\n" );
				
				
			
					
				//} // if

			} // try

			/*******************************************************************************
			*	The EndOfStreamExeception below is thrown when you reach end of the input
			*	stream (duh). At this point, the filter ports are closed and a message is
			*	written letting the user know what is going on.
			********************************************************************************/

			catch (EndOfStreamException e)
			{
				ClosePorts();
				System.out.print( "\n" + this.getName() + "::Sink Exiting; bytes read: " + bytesread );
				writer.close();
				break;

			} // catch

		} // while
		} catch (FileNotFoundException | UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
   } // run
}
