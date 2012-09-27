package simciv.ui.base;

import org.newdawn.slick.Color;

public class MessageBox extends Window
{
	protected PushButton closeButton;
	protected Label message;

	public MessageBox(WidgetContainer parent, int x, int y, int width, int height, String title)
	{
		super(parent, x, y, width, height, title);
		
		closeButton = new PushButton(this, 0, 0, "OK");
		closeButton.setMargins(0, 8);
		closeButton.setAlign(ALIGN_CENTER, ALIGN_BOTTOM);
		closeButton.addActionListener(new CloseAction());
		
		message = new Label(this, 4, 2, "---");
		message.setTextColor(Color.black);
		message.setSize(width, height - closeButton.getHeight());
		message.setTextWrap(true);
		
		add(closeButton);
		add(message);
	}
	
	public void setText(String text)
	{
		message.setText(text);
	}
	
	public void addCloseListener(IActionListener l)
	{
		closeButton.addActionListener(l); // Fof the push button
		addOnCloseAction(l); // For the title bar close button
	}
	
	class CloseAction implements IActionListener
	{
		@Override
		public void actionPerformed(Widget sender) {
			setVisible(false);
		}
	}

}

