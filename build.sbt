name := "play-sandboxxy"

version := "1.0-SNAPSHOT"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  javaJdbc,
  "postgresql"         %  "postgresql"      % "9.1-901-1.jdbc4", 
  "com.typesafe.slick" %% "slick"           % "1.0.1",
  "com.typesafe.play"  %% "play-slick"      % "0.5.0.8",
  "com.typesafe.slick" %% "slick-testkit"   % "1.0.0"           % "test",
  "com.novocode"       %  "junit-interface" % "0.10-M1"         % "test",
  "ch.qos.logback"     %  "logback-classic" % "0.9.28"          % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s", "-a")

parallelExecution in Test := false

logBuffered := false

play.Project.playScalaSettings
