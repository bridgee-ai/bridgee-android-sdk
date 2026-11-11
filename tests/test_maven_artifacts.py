"""Validate unsigned Gradle publication in the isolated build/maven-local repository."""
import pathlib
import unittest
import xml.etree.ElementTree as ET
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
VERSION = next(line.split("=", 1)[1].strip() for line in
               (ROOT / "gradle.properties").read_text().splitlines()
               if line.startswith("VERSION_NAME="))
ARTIFACT = "bridgee-android-sdk"
DIRECTORY = ROOT / "build/maven-local/ai/bridgee" / ARTIFACT / VERSION
PREFIX = DIRECTORY / f"{ARTIFACT}-{VERSION}"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}

class MavenArtifacts(unittest.TestCase):
    def test_pom_coordinates_metadata_and_dependencies(self):
        pom = ET.parse(str(PREFIX) + ".pom").getroot()
        for name, expected in [("groupId", "ai.bridgee"), ("artifactId", ARTIFACT),
                               ("version", VERSION), ("packaging", "aar")]:
            self.assertEqual(pom.findtext("m:" + name, namespaces=NS), expected)
        for path in ["name", "description", "url", "licenses/license/name", "licenses/license/url",
                     "developers/developer/id", "developers/developer/name", "developers/developer/email",
                     "scm/connection", "scm/developerConnection", "scm/url"]:
            self.assertTrue(pom.findtext("/".join("m:" + x for x in path.split("/")), namespaces=NS), path)
        dependencies = pom.findall("m:dependencies/m:dependency", NS)
        self.assertEqual({(d.findtext("m:groupId", namespaces=NS), d.findtext("m:artifactId", namespaces=NS),
                          d.findtext("m:version", namespaces=NS)) for d in dependencies},
                         {("com.google.code.gson", "gson", "2.10.1"),
                          ("com.android.installreferrer", "installreferrer", "2.2")})
    def test_aar_and_classifiers_have_license_and_real_contents(self):
        license_bytes = (ROOT / "LICENSE").read_bytes()
        for suffix, license_path, content in [
            (".aar", "assets/bridgee-LICENSE.txt", "classes.jar"),
            ("-sources.jar", "META-INF/LICENSE", "ai/bridgee/android/sdk/BridgeeSDK.java"),
            ("-javadoc.jar", "META-INF/LICENSE", "ai/bridgee/android/sdk/BridgeeSDK.html")]:
            with zipfile.ZipFile(str(PREFIX) + suffix) as jar:
                self.assertEqual(jar.read(license_path), license_bytes)
                self.assertGreater(len(jar.read(content)), 100)
                if suffix == "-javadoc.jar":
                    self.assertIn(b"javadoc", jar.read("index.html").lower())
                    self.assertIn(b"firstOpen", jar.read(content))
        self.assertEqual(list(DIRECTORY.glob("*.asc")), [])

if __name__ == "__main__":
    unittest.main()
