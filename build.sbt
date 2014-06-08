name := "play-sandboxxy"

version := "1.0-SNAPSHOT"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  javaJdbc,
  "postgresql"         %  "postgresql"      % "9.1-901-1.jdbc4", 
  "com.typesafe.slick" %% "slick"           % "2.0.2",
  "com.typesafe.play"  %% "play-slick"      % "0.6.0.1",
  "com.typesafe.slick" %% "slick-testkit"   % "2.0.1"           % "test",
  "com.novocode"       %  "junit-interface" % "0.10"            % "test",
  "ch.qos.logback"     %  "logback-classic" % "1.1.2"           % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s", "-a")

parallelExecution in Test := false

logBuffered := false

play.Project.playScalaSettings
