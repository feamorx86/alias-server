package ru.feamor.aliasserver.test;

import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.db.DBRequest;

public class LittleTest {
	
	public static double  ThreadWork() {
		double result = Integer.MAX_VALUE;
		for (int i=0; i < 10000; i++) {
			result = result + (double) i / (double)Integer.MAX_VALUE;  
		}
		return result;
	}
	
	public static void main(String[] args) {
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				for (int i = 0; i < 100; i++) {
					double result = 0;
					result = ThreadWork();
					
					System.out.println("result at ("+i+") is :"+result);
					
					if (Thread.interrupted()) {
						System.out.println("Interrupted at "+i);
						return;
					}
				}
			}
		});
		thread.start();
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		thread.interrupt();
				
		System.out.println("result complete");
	}

}
