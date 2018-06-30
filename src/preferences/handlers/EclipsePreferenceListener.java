package preferences.handlers;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.service.prefs.BackingStoreException;

public class EclipsePreferenceListener {

	public static final String PREFERENCE_CONSOLE = "preference.change";
	private IPreferenceChangeListener listener;
	private MessageConsole console;

	public void addPreferenceChangeListeners() {
		console = findConsole(PREFERENCE_CONSOLE);
		listener = new IPreferenceChangeListener() {
			@Override
			public void preferenceChange(PreferenceChangeEvent event) {
				MessageConsoleStream out = console.newMessageStream();
				String line = event.getNode() + "/" + event.getKey() + "=" + event.getNewValue();
				out.println(line);
				System.out.println(line);
			}
		};

		IPreferencesService prefsService = Platform.getPreferencesService();
		IEclipsePreferences root = prefsService.getRootNode();

		IPreferenceNodeVisitor addingVisitor = new IPreferenceNodeVisitor() {
			public boolean visit(IEclipsePreferences node) {
				if (null != listener) {
					node.addPreferenceChangeListener(listener);
					node.addNodeChangeListener(new INodeChangeListener() {
						@Override
						public void removed(NodeChangeEvent event) {
						}

						@Override
						public void added(NodeChangeEvent event) {
							IEclipsePreferences node = (IEclipsePreferences) event.getChild();
							node.addPreferenceChangeListener(listener);
						}
					});
				}
				return true;
			}
		};

		try {
			root.accept(addingVisitor);
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		//no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

	public void showConsoleView() {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
			view.display(console);
			
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
	}
}
