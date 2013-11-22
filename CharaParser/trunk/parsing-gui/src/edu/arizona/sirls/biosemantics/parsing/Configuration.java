package edu.arizona.sirls.biosemantics.parsing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import com.swtdesigner.SWTResourceManager;

public class Configuration {

	/**
	 * @param args
	 */
	
	static {
		//Set the Log File path
		try {
			//ApplicationUtilities.setLogFilePath();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Configuration().viewConfigurationForm();
	}
	

	
	/**
	 * @wbp.parser.entryPoint
	 */
	public void viewConfigurationForm () {
		final Display display = Display.getDefault();
		
		final Shell shell = new Shell(display);
		shell.setImage(SWTResourceManager.getImage(Configuration.class, "/edu/arizona/sirls/biosemantics/parsing/garland_logo.gif"));
		shell.setSize(793, 741);
		shell.setLocation(200, 20);
		shell.setText("Choose a document style");
	
		Group group = new Group(shell, SWT.NONE);
		group.setBounds(20, 10, 740, 65);
		
		Label label = new Label(group, SWT.NONE);
		label.setBounds(10, 23, 730, 32);
		label.setText("Please choose a type that closely matches the document you are using for semantic mark-up : (type 3 is under-construction)");
		
		Button button = new Button(group, SWT.RADIO);
		button.setBounds(527, 23, 90, 16);
		button.setText("Radio Button");
		
		Group group_1 = new Group(shell, SWT.NONE);
		group_1.setBounds(20, 98, 740, 597);
		
		Button type1 = new Button(group_1, SWT.RADIO);
		type1.setBounds(10, 29, 90, 16);
		type1.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent mEvent){
				shell.setVisible(false);
				new Type1Document().showType1Document();
				
				 do {
					 if (VolumeExtractor.getStart() == null) {
							String message = ApplicationUtilities.getProperty("popup.info.type1") + " ";
							ApplicationUtilities.showPopUpWindow(message + ApplicationUtilities.getProperty("popup.error.style"), 
									ApplicationUtilities.getProperty("popup.header.error"), SWT.ICON_ERROR);
							new Configuration().viewConfigurationForm();
					 }
					 
				} while (VolumeExtractor.getStart() == null &&!shell.isDisposed());
				 
				if (VolumeExtractor.getStart() != null) {
					if(!shell.isDisposed()) {
						shell.dispose();
					}
					
					MainForm.launchMarker("");
					System.exit(0);
				}	

			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		type1.setText("Type 1:");
		
		Button type2 = new Button(group_1, SWT.RADIO);
		type2.setBounds(10, 166, 90, 16);
		type2.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent mEvent){
				//new Type2Document().showType2Document();
				shell.setVisible(false);
				
				if(!shell.isDisposed()) {
					shell.dispose();
				}
				MainForm.launchMarker("type2");
				System.exit(0);
			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		type2.setText("Type 2:");
		
		//OCRed text: underconstruction
		Button type3 = new Button(group_1, SWT.RADIO);
		type3.setEnabled(false);
		type3.setBounds(10, 307, 90, 16);
		type3.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent mEvent){
				shell.setVisible(false);
				new Type3Document().showType3Document();
				if(!shell.isDisposed()) {
					shell.dispose();
				}
				MainForm.launchMarker("type3");
				System.exit(0);
			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		type3.setText("Type 3:");
		
		Label label_1 = new Label(group_1, SWT.BORDER | SWT.WRAP);
		label_1.setBackgroundImage(SWTResourceManager.getImage(Configuration.class, 
				ApplicationUtilities.getProperty("image.type2")));
		label_1.setBounds(106, 167, 611, 127);
		label_1.setToolTipText("Word Document with Content-Specific Styles");
		
		System.out.println(ApplicationUtilities.getProperty("image.type1"));
		Label label_2 = new Label(group_1, SWT.BORDER | SWT.WRAP);
		label_2.setBackgroundImage(SWTResourceManager.getImage(Configuration.class, 
				ApplicationUtilities.getProperty("image.type1")));
		label_2.setBounds(106, 30, 611, 121);
		label_2.setToolTipText("XML Document produced by CharaParser");
		
		Label label_3 = new Label(group_1, SWT.BORDER | SWT.WRAP);
		label_3.setBackgroundImage(SWTResourceManager.getImage(Configuration.class, 
				ApplicationUtilities.getProperty("image.type3")));
		label_3.setBounds(106, 308, 611, 127);
		label_3.setToolTipText("Text from an OCR Process");
		
		Button btnType = new Button(group_1, SWT.RADIO);
		//btnType.setBounds(10, 307, 90, 16);
		btnType.setBounds(10, 451, 71, 16);
		btnType.setText("Type 4:");
		btnType.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent mEvent){
				shell.setVisible(false);
				String xml = new Type4Document().showType4Document();
				if(!shell.isDisposed()) {
					shell.dispose();
				}
				MainForm.launchMarker("type4:"+xml);
				System.exit(0);
			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		
		Label label_4 = new Label(group_1, SWT.BORDER | SWT.WRAP);
		label_4.setBounds(106, 451, 611, 117);
		//label_3.setBounds(106, 308, 611, 127);
		label_4.setBackgroundImage(SWTResourceManager.getImage(Configuration.class, 
				ApplicationUtilities.getProperty("image.type4")));
		label_4.setToolTipText("NeXML or TaxonX Documents with Text Morphological Descriptions");
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
}
