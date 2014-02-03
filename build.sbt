import com.typesafe.sbt.packager.linux.LinuxSymlink
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import sbtrelease._
import ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.ReleaseKeys
import ReleaseKeys._

name := """hello-play"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  // Select Play modules
  //jdbc,      // The JDBC connection pool and the play.api.db API
  //anorm,     // Scala RDBMS Library
  //javaJdbc,  // Java database API
  //javaEbean, // Java Ebean plugin
  //javaJpa,   // Java JPA plugin
  //filters,   // A set of built-in filters
  //javaCore,  // The core Java API
  // WebJars pull in client-side web libraries
  "org.webjars" %% "webjars-play" % "2.2.0",
  "org.webjars" % "bootstrap" % "2.3.1"
  // Add your own project dependencies in the form:
  // "group" % "artifact" % "version"
)

play.Project.playScalaSettings


description in Linux := "Hello Play"

packageDescription in Linux := "Hello Play"

packageSummary := "Summary for Hello Play "

maintainer := "Vijay <vijay@lunatech.com>"

packageBin in Debian <<= (debianExplodedPackage in Debian, debianMD5sumsFile in Debian, target, streams) map { (pkgdir, _, tdir, s) =>
// Make the phackage.  We put this in fakeroot, so we can build the package with root owning files.
  Process(Seq("fakeroot", "--", "dpkg-deb", "-Zgzip", "--build", pkgdir.getAbsolutePath), Some(tdir)) ! s.log match {
    case 0 => ()
    case x => sys.error("Failure packaging debian file.  Exit code: " + x)
  }
  file(tdir.getAbsolutePath + ".deb")
}

sourceDirectory in Debian <<= (sourceDirectory) apply (_ / "debian")

debianMaintainerScripts in Debian <<= (sourceDirectory in Debian) map { srcDir =>
  ((srcDir / "DEBIAN") * "*").get map {f => (f, f.getName)}
}

releaseSettings

val createDebianPackage = (ref: ProjectRef) => ReleaseStep(
  action = releaseTask(packageBin in Debian in ref)
)

releaseProcess <<= thisProjectRef apply { ref =>
  Seq[ReleaseStep](
    checkSnapshotDependencies,              // : ReleaseStep
    inquireVersions,                        // : ReleaseStep
    runClean,
    runTest,                                // : ReleaseStep
    setReleaseVersion,                      // : ReleaseStep
    commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
    tagRelease,                             // : ReleaseStep
    createDebianPackage(ref),
    //  publishArtifacts,                       // : ReleaseStep, checks whether `publishTo` is properly set up
    setNextVersion,                         // : ReleaseStep
    commitNextVersion,                      // : ReleaseStep
    pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
  )
}
