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

1 - Install Java : https://adoptium.net

2 - Install Git : http://git-scm.com/download/

## How To Run

1- Download the repo and unzip it.

2- Open gitlet-master then open gitlet folder.

3- Open git bash and navigate to the path in step 3, use (cd ..) to go one directory to the back, (cd folder_name) to enter the specified folder and use (pwd) to know where you currently are. (NOTE:use the previous commands without the braces)

4- Compile the program by typing javac *.java

5- Run the program by typing java Main [THE COMMANDS GO HERE WITHOUT THE SQUARE BRACKETS EACH ARGUMENT SHOULD BE SEPARTED BY A SPACE OR CAPSULATE THE DESIRED ARGUMENT IN DOUBLE QUOTES]

6- Supported commands are listed below.

## Gitlet functionality
- init
