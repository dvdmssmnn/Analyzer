import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class TextOnlyLayout extends Layout {

	@Override
	public void activateOptions() {
	}

	@Override
	public String format(LoggingEvent arg0) {
		return String.format("%s\n", arg0.getMessage());
	}

	@Override
	public boolean ignoresThrowable() {
		return false;
	}

}
