package devrock.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;

import devrock.cicd.model.api.reason.CommandFailed;
import devrock.process.execution.ProcessExecution;

public class GitTools {
	
	public static Reason gitCreateLocalBranch(File path, String name) {
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "checkout", "-b", name);

		if (resultMaybe.isUnsatisfied())
			return resultMaybe.whyUnsatisfied();
		
		return null;
	}
	
	public static Reason gitAdd(File path) {
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "add", ".");
		
		if (resultMaybe.isUnsatisfied())
			return resultMaybe.whyUnsatisfied();
		
		return null;
	}
	
	public static Maybe<String> getLatestCommitComment(File path) {
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "log", "-1", "--pretty=%B");
		
		return resultMaybe;
	}
	
	public static Maybe<String> getBranchHash(File path, String remote, String branch) {
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "rev-parse", remote + "/" +branch);
		
		return resultMaybe;
	}
	
	public static Reason gitCommit(File path, String comment) {
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "commit", "--message", comment);
		
		if (resultMaybe.isUnsatisfied())
			return resultMaybe.whyUnsatisfied();
		
		return null;
	}
	
	public static Reason gitPush(File path, String remote, String targetBranch) {
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "push", remote, "HEAD:" + targetBranch);
		
		if (resultMaybe.isUnsatisfied())
			return resultMaybe.whyUnsatisfied();
		
		return null;
	}
	
	public static Reason gitCommitAll(File path, String comment) {
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "commit", "--all", "--message", comment);

		if (resultMaybe.isUnsatisfied())
			return resultMaybe.whyUnsatisfied();
		
		return null;
	}
	
	public static Maybe<String> getLatestCommitHash(File path, String fileOrDirectory) {
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "rev-list", "-n", "1", "HEAD", "--", fileOrDirectory);
		return resultMaybe;
	}
	
	public static boolean isGitCheckoutRoot(File path) {
		if (!path.exists())
			return false;
		
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "rev-parse", "--git-dir");
		
		if (resultMaybe.isUnsatisfiedBy(CommandFailed.T))
			return false;
		
		return resultMaybe.get().equals(".git");
	}
	
	//public static String get
	
	public static SortedSet<File> getChangedOrUntrackedFolders(File path, String hash) {
		SortedSet<File> changedOrUntrackedFiles = new TreeSet<>();
		
		changedOrUntrackedFiles.addAll(getChangedFolders(path, hash));
		changedOrUntrackedFiles.addAll(getUntrackedFolders(path));
		
		return changedOrUntrackedFiles;
	}
	
	public static List<File> getChangedFolders(File path, String hash) {
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "diff", "-r", hash, "--name-status");
		
		BufferedReader reader = new BufferedReader(new StringReader(resultMaybe.get()));
		
		String line = null;
		
		Set<String> itemsFound = new TreeSet<>();
		Set<String> rootItemsDeleted = new HashSet<>();
		
		try {
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				
				if (line.isEmpty())
					continue;
				
				char mode = line.charAt(0);
				
				Path itemPath = Paths.get(line.substring(1).trim());
				String itemName = itemPath.getName(0).toString();
				boolean isRootLevel = itemPath.getNameCount() == 1;
				
				switch (mode) {
				case 'A':
				case 'M':
					itemsFound.add(itemName);
					break;
				case 'D':
					if (isRootLevel) {
						rootItemsDeleted.add(itemName);
					}
					else {
						itemsFound.add(itemName);
					}
					break;
				default:
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
		itemsFound.removeAll(rootItemsDeleted);

		List<File> changedFolders = new ArrayList<>(itemsFound.size());
		
		for (String item: itemsFound) {
			File file = new File(path, item);
			if (file.isDirectory())
				changedFolders.add(file);
		}
		
		return changedFolders;
	}
	
	public static List<File> getUntrackedFolders(File path) {
		Maybe<String> resultMaybe = ProcessExecution.runCommand(path, "git", "ls-files", "--exclude-standard", "--others");

		BufferedReader reader = new BufferedReader(new StringReader(resultMaybe.get()));
		
		String line = null;
		
		Set<String> itemsFound = new TreeSet<>();
		
		try {
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				
				if (line.isEmpty())
					continue;
				
				Path itemPath = Paths.get(line);
				itemsFound.add(itemPath.getName(0).toString());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		List<File> untrackedFolders = new ArrayList<>(itemsFound.size());
		
		for (String item: itemsFound) {
			File file = new File(path, item);
			if (file.isDirectory())
				untrackedFolders.add(file);
		}
		
		return untrackedFolders;
	}
}
