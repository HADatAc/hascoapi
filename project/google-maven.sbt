// Use Google's Maven mirror to avoid rate limiting
// Google mirrors Maven Central without rate limits

import sbt._
import Resolver._

externalResolvers := Seq(
  "Google Maven Central" at "https://maven-central-storage.googleapis.com/maven2/",
  "Google Maven Mirror" at "https://maven-central.storage-download.googleapis.com/maven2/",
  "Typesafe" at "https://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)
