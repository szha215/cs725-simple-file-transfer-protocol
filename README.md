# CS725 Assignment 1 - SFTP

John Zhang - szha215 - 8605362

All commands completed. Currently runs on `localhost`.

- Multi-client support - The implemented SFTP supports multiple clients, as the server forks a thread.
- Directory protection - The client cannot change directory to outside of allocated space, i.e. `cdir ..` at the root `~` is not allowed.

## Run Instructions

1. Open Eclipse.

2. Import Existing General Project `cs725_ass1`.

   ![00_import_button](C:\Users\johnz\Desktop\725\git\screenshots\00_import_button.png)

   ​

   ![01_import_project_1](C:\Users\johnz\Desktop\725\git\screenshots\01_import_project_1.png)

   ![01_import_project_2](C:\Users\johnz\Desktop\725\git\screenshots\01_import_project_2.png)

   ​

   ​

   ​

   ​

3. Run `server.Server`.

   ![02_run_server](C:\Users\johnz\Desktop\725\git\screenshots\02_run_server.png)

   ​

   ​

4. Run `client.Client`.

   ![03_run_client](C:\Users\johnz\Desktop\725\git\screenshots\03_run_client.png)

   ​

   ​

5. Enter commands in the Client console.

   ![04_both_running](C:\Users\johnz\Desktop\725\git\screenshots\04_both_running.png)

   ​

   ​

6. Enter `DONE` when finished with the file transfers.

Screenshots are in `screenshots/`.

### Logging in

There are currently 5 pre-loaded accounts for testing, stored in `res/userlist.json`:

```reStructuredText
User ID: AA
Account: aa
Password: 11
```

```reStructuredText
User ID: BB
Account: bb
Password: 22
```

```reStructuredText
User ID:
Account: cc
Password: 33
```

```reStructuredText
User ID: 
Account: dd
Password: 22
```

```reStructuredText
User ID: admin
Account:
Password:
```

To log in, an Account and its respective Password are required. User ID is only used to check whether it exists on the server or not, except for **User`admin`, which grants log-in without an Account or a Password.**

Whenever the server replies a "-" message, the user will need to log in again.

The `$` in examples indicate what the user has typed into console, do not include them.



### Provided directory structure

The server and client will run with `res/ServerFolder/` and `res/ClientFolder/` as its default directory respectively. All client retrieved files go to its default directory, while server stored files can go to any sub-directory in its default directory.

![00_directory](C:\Users\johnz\Desktop\725\git\screenshots\00_directory.png)



## Commands

### `USER` `<user-id>`

 Checks whether `user-id` exists on the server, does not grant log-in (except for `admin`).

Example:

```bash
$ USER AA
+User-id valid, send account and password
```



### `ACCT` `<account>`

The account you want to use to access the SFTP server.

Example:

```bash
$ ACCT aa
+Account valid, send password
```



### `PASS` `<password>`

The password associated with your account, can be sent prior to `ACCT`.

Example:

```bash
$ PASS 11
! Logged in
```



Since multiple accounts may share the same password, entering either `ACCT bb` or `ACCT dd` after `PASS 22` can log in.

###  `TYPE` `{ A | B | C }`

Changes the mapping of the stored file and transmission byte stream, default is binary. Requires the user to be logged in.

Example:

```bash
$ TYPE C
+Using Continuous mode
```



###  `LIST` `{ F | V }` `<directory>`

Lists all files and folders in `<directory>`. If `<directory>` is not specified, the current working directory will be listed. Requires the user to be logged in.

`F` specifies the standard directory listing, only filenames are listed.

`V` specifies a verbose directory listing, including filenames, last modified date/time, file size in bytes and the owner of the file.

Examples:

Listing the current directory in verbose mode:

```bash
$ LIST V
+
./
../
f1/            25/08/2017 14:27         0     JOHN-XPS\johnz
f2/            26/08/2017 18:56         0     JOHN-XPS\johnz
t.txt          26/08/2017 19:57        13     JOHN-XPS\johnz
text.txt       26/08/2017 19:57        13     JOHN-XPS\johnz
text1.txt      25/08/2017 16:11        49     JOHN-XPS\johnz
text2.txt      25/08/2017 16:10         2     JOHN-XPS\johnz
uoa.png        30/07/2017 12:10      5300     JOHN-XPS\johnz
```



Listing `f1/` sub-directory in non-verbose mode:

```bash
$ LIST F f1
+
./
../
f11/ 
f12/ 
text11.txt 
```



###  `CDIR` `<new-directory>`

Changes the current working directory. Changing the directory to somewhere out of allocated directory is not allowed. If the user is not logged in, the server will prompt the user to log in.

Directory can be relative to `~`.

Example:

Changing directory to `f2/`:

```bash
$ CDIR f2
!Changed working dir to ~\f2
```



Changing directory using `~`, from `~/f2` to `~/f1/f12/` :

```bash
$ CDIR ~/f1/f12
!Changed working dir to ~\f1\f12
```



Changing directory to `~`, and attempting to go up one level:

```bash
$ CDIR ~
!Changed working dir to ~
$ CDIR ..
-Can't connect to directory because permission denied
```





###  `KILL` `<filename>`

Deletes the specified `<filename>` in the current working directory. Requires the user to be logged in.

Example:

```bash
$ KILL text.txt
+test.txt deleted
```



###  `NAME` `<filename>`

Renames `<filename>`. If server responds with `+`, the user should reply with `TOBE` `<new-filename>`. Requires the user to be logged in.

Example:

Changing the filename of `t.txt` to `t2.txt`:

```bash
$ NAME t.txt
+File exists
$ TOBE t2.txt
+t.txt renamed to t2.txt
```



###  `DONE`

Disconnect from server.

Example:

```bash
$ DONE
+CS725 closing connection...
```



###  `RETR` `<filename>`

Request that the server send the specified file. If the file doesn't fit on the client's system, it will not be sent. Requires the user to be logged in.

Example:

Retrieving `uoa.png` from server:

```bash
$ RETR uoa.png
File size is 5300 bytes
Waiting for file...
File uoa.png received
```



Directory within `<filename>` is not allowed:

```bash
$ RETR f1/text11.txt
Invalid filename
```



###  `STOR` ` { NEW | OLD | APP }` `<filename>`

Request the server to receive file `<filename>` with a specific write mode. Requires the user to be logged in.

`NEW` specifies it should try to create a new file. If it already exists, the server will return with `-`, otherwise `+`.

`OLD` specifies that it should overwrite existing file, or make a new file if it doesn't exist. Always return `+`.

`APP` specifies that it should append to existing file, or make a new file if it doesn't exist. Always return `+`.

Directory within `<filename>` is not allowed.

Example:

Store `301.jpg` (2.5MB+ file) to server in mode NEW:

```bash
$ STOR NEW 301.jpg
+Will create file
+ok, waiting for file
+Saved 301.jpg
```



Store `client.txt` to server, overwriting old:

```bash
$ STOR OLD client.txt
+Will create file
+ok, waiting for file
+Saved client.txt
```



Append `client.txt` to server:

```bash
$ STOR APP client.txt
+Will append to file
+ok, waiting for file
+Saved client.txt
```




