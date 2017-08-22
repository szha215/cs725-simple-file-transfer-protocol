# CS725 Assignment 1 - SFTP

John Zhang - szha215 - 8605362

## Run Instructions

1. Open Eclipse.
2. Import General Project `cs725_ass1`.
3. Run `server.Server`.
4. Run `client.Client`.
5. Enter commands in the Client console.
6. Enter `DONE` when finished with the file transfers.

## Usage

### Log in

There are currently 3 pre-loaded accounts for testing, stored in `res/userlist.json`. 

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
User ID: admin
Account:
Password:
```

To log in, an Account and its respective Password are required. User ID is only used to check whether it exists on the server or not, except for **User`admin`, which grants log-in without an Account or a Password.**

### Get file from server



### Upload file to server



## Commands

- `USER` `<user-id>`

 Checks whether `user-id` exists on the server, does not grant log-in (except for `admin`).



- `ACCT` `<account>`

The account you want to use to access the SFTP server.



- `PASS` `<password>`

The password associated with your account, can be sent prior to `ACCT`.