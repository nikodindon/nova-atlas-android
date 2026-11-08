"""Offline reproductions against the untouched baseline; no server access."""
import pathlib, unittest
root=pathlib.Path(__file__).resolve().parents[1]
class Baseline(unittest.TestCase):
 def test_polling_overlaps(self):
  text=(root/'app/src/main/java/com/niko/novaatlas/AppScreen.kt').read_text()
  self.assertIn('delay(2000)', text); self.assertIn('scope.launch', text)
  # A request taking 7 seconds overlaps four two-second admissions.
  self.assertEqual(sum(t < 7 for t in range(0,8,2)),4)
 def test_release_falls_back_to_debug(self):
  self.assertIn('signingConfigs.getByName("debug")',(root/'app/build.gradle.kts').read_text())
if __name__=='__main__': unittest.main()
