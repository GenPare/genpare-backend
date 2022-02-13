# GenPare: Backend

This repository is concerned with the backend of the web application GenPare. It is written in Kotlin using
[Ktor](https://ktor.io). It also uses a MariaDB instance.

## Running with Docker Compose

### Prerequisites:

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)
- A JDK

### Building the image

#### Step 0: Remove an existing image

If you already have a backend image present, you need to remove it, or changes won't be applied.

```bash
docker-compose rm -vf
docker image rm genpare-backend_genpare-backend
```

#### Step 1: Build the necessary files

Open a terminal in the project root and run the Gradle `installDist` task.

```bash
# Windows
gradlew.bat installDist

# *nix
./gradlew installDist
```

This step will take quite some time, as Gradle will download all necessary libraries and compile the backend, so grab
a coffee for this part.

#### Step 2:

Now you can let Docker Compose do the rest for you.

```bash
docker-compose up
```

The first time you execute this command, Docker Compose will build the image. Every subsequent time, it will just start
the containers without rebuilding them.

## Setting up a development environment

This section explains how to set up a local development environment.

### TL;DR:

- Clone the repo
- Open it in IDEA
- Check that you're using Java SDK 16
- Create a MariaDB database and user:

```sql
CREATE DATABASE `genpare`;
CREATE USER 'genpare' IDENTIFIED BY 'testing123';
GRANT USAGE ON *.* TO 'genpare'@localhost IDENTIFIED BY 'testing123';
GRANT ALL PRIVILEGES ON `genpare`.* TO 'genpare'@localhost;
FLUSH PRIVILEGES;
```

- Run the `run` Gradle task in the `application` folder, or alternatively, run the Application.kt file

### More infos, please.

#### Dependencies

To run the backend from the source code, you need to have a few things installed.

1. [IntelliJ IDEA](https://www.jetbrains.com/de-de/idea/)
2. [MariaDB](https://mariadb.org/download/) (Please make sure the port is kept at `3306`, the default one!)

#### Downloading the source code

After opening IDEA, click on `File` > `New` > `Project from Version Control...`

![File > New > Project from Version Control...](https://egirl.rip/zAd3xxkJTJ.png?key=s5ougQY486bZlM)

In the resulting dialog, enter the Git URL of this repository (https://github.com/GenPare/genpare-backend.git) and a
directory to clone the repo to.

![Enter the Git URL](https://egirl.rip/PLTQYBainX.png?key=oByhmVyltYh3CF)

Then, click `Clone`. After this, IDEA will clone the repository. It will also ask you if you want to trust the
repository. You can always check the
[build.gradle.kts](https://github.com/GenPare/genpare-backend/blob/main/build.gradle.kts) file to check if you actually
want to trust the project. This is an important safety feature of IDEA, so please always make sure to preview unknown
projects in Safe Mode. If you do trust us, click `Trust Project`. Else, click `Preview in Safe Mode`. Do keep in mind
however that you will need to trust the project at some point to actually build it.

![Trust the Gradle project](https://egirl.rip/wNMiucRm1c.png?key=TOfoHhNmDDPEY5)

At this point, Gradle will automatically download all required dependencies and cache them locally, so they don't need
to be downloaded again. After the Gradle project was fully imported, please check your SDK version to ensure the built
files are built with Java 16. For this, go to `File` > `Project Structure...` > `Project Settings` > `Project`. Here,
the selected project SDK should be 16. Relevant is the number written in gray text to the right, not the identifier to 
the left written in white text. If not, change the version to be 16.

![Check your Java SDK level to be 16!](https://egirl.rip/3Gk1AQJive.png?key=UUGpgb9ZKS9sSe)

If you don't have a Java 16 SDK, you can also download one from within IDEA.

![Download a Java 16 SDK, if required.](https://egirl.rip/tgtb8Cznmv.png?key=tKzX1IOHu6DUoD)

#### Setting up the database

Make sure you have installed MariaDB. Also, please make sure that the database is running on port `3306`.

Open the MariaDB console from the start menu. Then, run the following commands:

```sql
CREATE DATABASE `genpare`;
CREATE USER 'genpare' IDENTIFIED BY 'testing123';
GRANT USAGE ON *.* TO 'genpare'@localhost IDENTIFIED BY 'testing123';
GRANT ALL PRIVILEGES ON `genpare`.* TO 'genpare'@localhost;
FLUSH PRIVILEGES;
```

They create a new database called `genpare`, a new user `genpare` with the password `testing123`, grant permissions to
this user for only the newly created database, and lastly flush these permissions for them to take effect immediately.

#### Running the server

When you're done with all these steps, you can run the server application. You have two options:

1. Click on the Gradle tab on the right-hand side and go to `genpare-backend` > `Tasks` > `application`. Here, you can
just double-click `run`.

![Run the "run" Gradle task.](https://egirl.rip/vCQA5TE6jV.png?key=aBqFSLQRyQ0PqC)

2. In the project view on the left-hand side, go to `genpare-backend` > `src` > `main` > `kotlin` > `de.genpare` >
`Application.kt`.

![Open the Application.kt file.](https://egirl.rip/WW7EQiUZk5.png?key=MfisljCQMqdcOa)

In the gutter, you'll see a green arrow. You can click it to run the application, too.

![Click the green arrow.](https://egirl.rip/iGaC5fcoKh.png?key=URVBmXz7dEtPpi)
