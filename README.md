# Gitlet
## Overview
A version-control system is essentially a backup system for related collections of files. The main functionality that Gitlet supports is:

- Saving the contents of entire directories of files. In Gitlet, this is called committing, and the saved contents themselves are called commits.

- Restoring a version of one or more files or entire commits. In Gitlet, this is called checking out those files or that commit.

- Viewing the history of your backups. In Gitlet, you view this history in something called the log.

- Maintaining related sequences of commits, called branches.

- Merging changes made in one branch into another.

In Gitlet, you don’t just commit individual files at a time. Instead, you can commit a coherent set of files at the same time. We like to think of each commit as a snapshot of your entire project at one point in time.
Maintaing a commit history of all versions of the project
      <p align="center">       ![image](https://github.com/user-attachments/assets/d7d364c7-41ab-42ac-a4b3-49f0e7920a7d) </p>

In Gilet we support branching and have different history of commits derived from the same commit , also we support merging them in one new commit.
      <p align="center">       ![image](https://github.com/user-attachments/assets/eb11b532-fd33-48a7-a2a4-3b3417be10a0)
 </p>
 
## Dependencies

1 - Install Java : https://www.java.com/en/download/


## How To Run

1- Download the repo and unzip it.

2- Open gitlet-master then open gitlet folder.

3- Open git bash and navigate to the path in step 3, use (cd ..) to go one directory to the back, (cd folder_name) to enter the specified folder and use (pwd) to know where you currently are. (NOTE:use the previous commands without the braces)

4- Compile the program by typing javac *.java

5- Run the program by typing java Main [THE COMMANDS GO HERE WITHOUT THE SQUARE BRACKETS EACH ARGUMENT SHOULD BE SEPARTED BY A SPACE OR CAPSULATE THE DESIRED ARGUMENT IN DOUBLE QUOTES]

6- Supported commands are listed below.

## Gitlet functionality
- ### <b>init</b> :
  
  Creates a new Gitlet version-control system in the current directory.

  This system will automatically start with one commit: a commit that contains no files and has the commit message initial commit (just like that, with no punctuation).

  It will have a single branch: master, which initially points to this initial commit, and master will be the current branch.

  The timestamp for this initial commit will be 00:00:00 UTC, Thursday, 1 January 1970 , Since the initial commit in all repositories created by Gitlet will have exactly the same content, it follows that all repositories will automatically share this commit (they will all have the same UID) and all commits in all repositories will trace back to it.


- ### <b>add </b>:

    Adds a copy of the file as it currently exists to the staging area.

    If the current working version of the file is identical to the version in the current commit, gitlet will not stage it to be added.

- ### <b> commit :</b>
     Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, creating a new commit.

     The commit is said to be tracking the saved files. By default, each commit’s snapshot of files will be exactly the same as its parent commit’s snapshot of files; it will keep versions of files exactly as they are, and not update them.

     A commit will only update the contents of files it is tracking that have been staged for addition at the time of commit, in which case the commit will now include the version of the file that was staged instead of the version it got from its parent.

     A commit will save and start tracking any files that were staged for addition but weren’t tracked by its parent. Finally, files tracked in the current commit may be untracked in the new commit as a result being staged for removal

     The staging area is cleared after a commit.

     The commit command never adds, changes, or removes files in the working directory (other than those in the .gitlet directory). The rm command will remove such files, as well as staging them for removal, so that they will be untracked after a commit.

     Any changes made to files after staging for addition or removal are ignored by the commit command, which only modifies the contents of the .gitlet directory. For example, if you remove a tracked file using the Unix rm command (rather than Gitlet’s command of the same name), it has no 
     effect on the next commit, which will still contain the (now deleted) version of the file.

     After the commit command, the new commit is added as a new node in the commit tree

  ![image](https://github.com/user-attachments/assets/301bd84d-27aa-43e6-9636-3b9979b1a771)

- ### <b> rm :</b>
 
    Unstage the file if it is currently staged for addition. If the file is tracked in the current commit, Gitlet stage it for removal and remove the file from the working directory if the user has not already done.

- ### <b> log :</b>

    Starting at the current head commit, display information about each commit backwards along the commit tree until the initial commit , but only consider first parent in case more than one parent.

  Example:
  
  ![image](https://github.com/user-attachments/assets/be7bd817-28b1-481a-8429-e253c42dea86)

  what the command do and get

   ![image](https://github.com/user-attachments/assets/5652cb54-338e-4990-84aa-9d19e8cb68ef)

- ### <b> global-log: </b>

   Like log, except displays information about all commits ever made. The order of the commits does not matter

- ### <b> find: </b>

   Prints out the ids of all commits that have the given commit message, one per line. If there are multiple such commits, it prints the ids out on separate lines

- ### <b> status: </b>

   display some statistics

  ![image](https://github.com/user-attachments/assets/56a2a698-c74a-468d-ae95-baf19469fec2)
