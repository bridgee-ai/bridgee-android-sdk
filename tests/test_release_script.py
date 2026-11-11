import pathlib, subprocess, unittest
ROOT = pathlib.Path(__file__).resolve().parents[1]
class ReleaseSafety(unittest.TestCase):
    def test_zip_stops_before_toolchain_or_signing(self):
        result = subprocess.run(["bash", str(ROOT / "create-bundle.sh"), "zip"], env={"PATH": "/usr/bin:/bin", "GRADLE_COMMAND": "/nonexistent/never-run"}, text=True, capture_output=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn("disabled", result.stderr)
        self.assertNotIn("No such file", result.stderr)
    def test_no_implicit_signing_or_manual_pom(self):
        source = (ROOT / "create-bundle.sh").read_text()
        self.assertNotIn("gpg --", source)
        self.assertNotIn("--gen-key", source)
        self.assertNotIn("<project", source)
        self.assertIn(":bridgeesdk:sourcesJar", source)
        self.assertIn(":bridgeesdk:javadocJar", source)
        self.assertIn(":bridgeesdk:generatePomFileForMavenPublication", source)
if __name__ == "__main__": unittest.main()
