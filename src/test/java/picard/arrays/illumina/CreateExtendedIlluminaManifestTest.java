package picard.arrays.illumina;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CreateExtendedIlluminaManifestTest {
    private static final Path TEST_DATA_DIR = Paths.get("testdata/picard/arrays/illumina/");

    @Test
    public void testFlagDuplicates() throws IOException {
        final File extendedManifestFile = TEST_DATA_DIR.resolve("GDA-8v1-0_A5.2.0.extended.csv").toFile();
        Build37ExtendedIlluminaManifest extendedManifest = new Build37ExtendedIlluminaManifest(extendedManifestFile);
        final List<Build37ExtendedIlluminaManifestRecord> records = new ArrayList<>();

        final Iterator<Build37ExtendedIlluminaManifestRecord> iterator = extendedManifest.extendedIterator();
        while (iterator.hasNext()) {
            records.add(iterator.next());
        }
        final Map<String, Float> nameToGentrainScore = new HashMap<>();
        // These two are simple SNPs in the manifest at the same chr/pos
        nameToGentrainScore.put("1:100343253-CT", 0.761f);      // Note - this is the 2nd entry in the manifest
        nameToGentrainScore.put("rs199660743", 0.887f);

        // These three exist in the manifest at the same chr/pos
        // This one is a SNP (so different alleles than the next two) and so is not flagged as a dupe
        nameToGentrainScore.put("rs118203419", 0.808f);
        // These two are indels, and although on different strands, have the same alleles.
        // So one will be flagged as a duplicate.  That will be the one with the lower GenTrain score
        nameToGentrainScore.put("9:132921819_IlmnFwd", 0.823f);     // Note - this is the 5th entry in the manifest
        nameToGentrainScore.put("9:132921819_IlmnRev", 0.848f);

        final CreateExtendedIlluminaManifest clp = new CreateExtendedIlluminaManifest();
        Set<Integer> duplicateIndices = clp.flagDuplicates(records, nameToGentrainScore);
        Assert.assertEquals(duplicateIndices.size(), 2);
        Assert.assertTrue(duplicateIndices.contains(1));        // Finds the 2nd entry (0-based)
        Assert.assertTrue(duplicateIndices.contains(4));        // Finds the 5th entry (0-based)
        int index = 0;
        for (Build37ExtendedIlluminaManifestRecord record : records) {
            if (!record.isFail() && record.isDupe()) {
                Assert.assertTrue(duplicateIndices.contains(index));
            } else {
                Assert.assertFalse(duplicateIndices.contains(index));
            }
            index++;
        }
    }
}
