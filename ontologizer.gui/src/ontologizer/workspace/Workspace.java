package ontologizer.workspace;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Workspace implements IResource
{
	private File defaultDirectory;

	private List<Project> projects = new ArrayList<Project>();

	public Workspace(File defaultDirectory)
	{
		setDefaultDirectory(defaultDirectory);
	}

	public void setDefaultDirectory(File defaultDirectory)
	{
		if (!defaultDirectory.isDirectory())
			throw new IllegalArgumentException();

		this.defaultDirectory = defaultDirectory;

		refresh();
	}

	public File getDefaultDirectory()
	{
		return defaultDirectory;
	}

	/**
	 * Add a new project that is located in the workspace and represent an object of it.
	 *
	 * @param projectName the name or folder name of the project relative to the default directory.
	 * @return the project
	 */
	public Project addProject(String projectName)
	{
		Project project = new Project(new File(defaultDirectory, projectName));
		projects.add(project);
		return project;
	}

	private void refresh()
	{
		String [] projectNames = defaultDirectory.list(new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				if (name.startsWith(".")) return false;
				return true;
			}
		});
		Arrays.sort(projectNames,String.CASE_INSENSITIVE_ORDER);

		for (String project : projectNames)
		{
			if (project.equals(".cache"))
				continue;

			addProject(project);
		}
	}
	public Iterable<Project> projects()
	{
		return projects;
	}
}
