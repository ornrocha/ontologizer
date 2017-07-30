[![Build Status](https://travis-ci.org/sba1/ontologizer.svg?branch=master)](https://travis-ci.org/sba1/ontologizer)

Ontologizer
===========

Ontologizer is a tool for identifying enriched Gene Ontology terms in lists of names
of genes or gene products. It is available as a Java program for both UI and
command line usage as well as JavaScript version that can be conveniently run in
modern Web browsers. Look at http://ontologizer.de for details and binary
distribution.

Building
--------

Ontologizer's build process is driven by Maven. After cloning and checking out the
source repository via
```
$ git clone https://github.com/sba1/ontologizer
```

Install pandoc (https://pandoc.org/) before proceeding. (The Pandoc dependency is not handled by Maven)

you can start the build procedure via

```
$ cd ontologizer.parent
$ mvn package
```

When the build breaks due to bugs in Javadocs, add ```-Dmaven.javadoc.skip=true``` to the
previous command and file an Issue. Similiary, if tests break the build you can add
```-DskipTests``` to skip the execution of the tests.

If successful, the command line version can be found in ```ontologizer.cmdline``` and the
UI version in the ```ontologizer.gui``` modules in the respective ```target``` folders.

For instance, still being in the ```ontologizer.parent``` folder, you can start the GUI
version by

```
$ cd ../ontologizer.gui/target
$ java -jar Ontologizer-jar-with-dependencies.jar
```

The ```Ontologizer-jar-with-dependencies.jar``` includes all dependencies, incl. SWT
for your platform.

API
---

The core of Ontologizer can be in principle used by other applications, although the API
is rather dumb. Snapshots are regulary deployed in a dedicated GitHub repo and can be accessed
via Maven in your ```pom.xml``` in the following way:

```
	<repositories>
		...
		<repository>
			<id>ontologizer-mvn</id>
			<url>https://raw.githubusercontent.com/sba1/ontologizer-mvn/master/</url>
			<snapshots>
				<enabled>true</enabled>
				<updatePolicy>always</updatePolicy>
			</snapshots>
		</repository>
		...
	</repositories>
```

Further more, add

```
	<dependencies>
		...
		<dependency>
			<groupId>de.ontologizer</groupId>
			<artifactId>ontologizer</artifactId>
			<version>0.0.3-SNAPSHOT</version>
		</dependency>
		...
	</dependencies>
```

to your ```pom.xml``` file to let your project depend on the Ontologizer core.
Notice that the ```groupId``` is slightly different than it is used in this
project (```de.ontologizer``` vs ```ontologizer```). Also note that the URL for
the repository is subject to change.
