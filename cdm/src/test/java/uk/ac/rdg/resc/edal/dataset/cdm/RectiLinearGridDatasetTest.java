package uk.ac.rdg.resc.edal.dataset.cdm;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.domain.Extent;
import uk.ac.rdg.resc.edal.domain.MapDomain;
import uk.ac.rdg.resc.edal.domain.MapDomainImpl;
import uk.ac.rdg.resc.edal.feature.DiscreteFeature;
import uk.ac.rdg.resc.edal.feature.GridFeature;
import uk.ac.rdg.resc.edal.feature.MapFeature;
import uk.ac.rdg.resc.edal.feature.PointSeriesFeature;
import uk.ac.rdg.resc.edal.feature.ProfileFeature;
import uk.ac.rdg.resc.edal.feature.TrajectoryFeature;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.grid.GridCell2D;
import uk.ac.rdg.resc.edal.grid.RectilinearGrid;
import uk.ac.rdg.resc.edal.grid.RegularGridImpl;
import uk.ac.rdg.resc.edal.grid.VerticalAxis;
import uk.ac.rdg.resc.edal.grid.VerticalAxisImpl;
import uk.ac.rdg.resc.edal.grid.TimeAxis;
import uk.ac.rdg.resc.edal.grid.TimeAxisImpl;
import uk.ac.rdg.resc.edal.metadata.VariableMetadata;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.position.VerticalCrs;
import uk.ac.rdg.resc.edal.position.VerticalCrsImpl;
import uk.ac.rdg.resc.edal.util.Array;
import uk.ac.rdg.resc.edal.util.Array1D;
import uk.ac.rdg.resc.edal.util.Array2D;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.Array4D;
import uk.ac.rdg.resc.edal.util.GridCoordinates2D;
import uk.ac.rdg.resc.edal.util.PlottingDomainParams;

public class RectiLinearGridDatasetTest {
    private static final double delta = 1e-5;
    private Dataset dataset;

    // parameters about the used test dataset
    private int xSize = 36;
    private int ySize = 19;
    private int tSize = 10;
    private int zSize = 11;

    private RectilinearGrid rGrid;
    private CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
    private Chronology chrnology;
    private VerticalCrs vCrs;
    private Extent<DateTime> datasetTExtent;
    private Extent<Double> datasetZExtent;
    private TimeAxis tAxis;
    private VerticalAxis vAxis;

    @Before
    public void setUp() throws Exception {
        URL url = this.getClass().getResource("/rectilinear_test_data.nc");
        String location = url.getPath();
        CdmGridDatasetFactory datasetFactory = new CdmGridDatasetFactory();
        dataset = datasetFactory.createDataset("testdataset", location);
        rGrid = new RegularGridImpl(-185.0, -95.0, 175.0, 95.0, crs, xSize, ySize);
        vCrs = new VerticalCrsImpl("m", false, false, false);
        chrnology = ISOChronology.getInstance();
        DateTime start = new DateTime(2000, 01, 01, 00, 00, chrnology);
        DateTime end = new DateTime(2000, 01, 10, 00, 00, chrnology);
        datasetTExtent = Extents.newExtent(start, end);
        datasetZExtent = Extents.newExtent(0.0, 100.0);
        List<DateTime> tAxisValues = new ArrayList<>();
        List<Double> zAxisValues = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tAxisValues.add(new DateTime(2000, 01, 01 + i, 00, 00, chrnology));
            zAxisValues.add(10.0 * i);
        }
        zAxisValues.add(100.0);
        tAxis = new TimeAxisImpl("time", tAxisValues);
        vAxis = new VerticalAxisImpl("depth", zAxisValues, vCrs);
    }

    // test metadata of the dataset
    @Test
    public void testMetadataInfo() throws Exception {
        assertEquals(chrnology, dataset.getDatasetChronology());
        assertEquals(vCrs, dataset.getDatasetVerticalCrs());
        assertEquals(MapFeature.class, dataset.getMapFeatureType("vLon"));

        VariableMetadata metadata = dataset.getVariableMetadata("vLon");

        assertEquals(vCrs, metadata.getVerticalDomain().getVerticalCrs());
        assertEquals(datasetZExtent, metadata.getVerticalDomain().getExtent());
        assertEquals(rGrid, metadata.getHorizontalDomain());
        assertEquals(datasetTExtent, metadata.getTemporalDomain().getExtent());
        assertTrue(metadata.isScalar());
    }

    // test if the result is null when arguments in PlottingdomainParams are set
    // out of
    // the domains of T and Z.
    @Test
    public void testNullTimeSerieFeatures() throws Exception {
        DateTime start = new DateTime(1990, 01, 01, 00, 00, chrnology);
        DateTime end = new DateTime(1998, 01, 01, 00, 00, chrnology);
        Extent<DateTime> tExtent = Extents.newExtent(start, end);

        for (Double zPos = 0.0; zPos <= 100; zPos += 10.0) {
            PlottingDomainParams params = new PlottingDomainParams(xSize, ySize,
                    rGrid.getBoundingBox(), null, tExtent, null, zPos, null);
            Collection<? extends PointSeriesFeature> timeSeriesFeature = dataset
                    .extractTimeseriesFeatures(null, params);
            assertEquals(0, timeSeriesFeature.size());
        }

        start = new DateTime(2010, 01, 01, 00, 00, chrnology);
        end = new DateTime(2012, 01, 01, 00, 00, chrnology);
        tExtent = Extents.newExtent(start, end);
        for (Double zPos = 0.0; zPos <= 100; zPos += 10.0) {
            PlottingDomainParams params = new PlottingDomainParams(xSize, ySize,
                    rGrid.getBoundingBox(), null, tExtent, null, zPos, null);
            Collection<? extends PointSeriesFeature> timeSeriesFeature = dataset
                    .extractTimeseriesFeatures(null, params);
            assertEquals(0, timeSeriesFeature.size());
        }

        start = new DateTime(2000, 1, 1, 00, 00, chrnology);
        end = new DateTime(2000, 1, 10, 00, 00, chrnology);
        tExtent = Extents.newExtent(start, end);
        Extent<Double> zExtent = Extents.newExtent(200.0, 500.0);
        PlottingDomainParams params = new PlottingDomainParams(xSize, ySize,
                rGrid.getBoundingBox(), zExtent, tExtent, null, null, null);
        Collection<? extends PointSeriesFeature> timeSeriesFeature = dataset
                .extractTimeseriesFeatures(null, params);
        assertEquals(0, timeSeriesFeature.size());

        zExtent = Extents.newExtent(-200.0, -100.0);
        params = new PlottingDomainParams(xSize, ySize, rGrid.getBoundingBox(), zExtent, tExtent,
                null, null, null);
        timeSeriesFeature = dataset.extractTimeseriesFeatures(null, params);
        assertEquals(0, timeSeriesFeature.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapFeaturesWithWrongParams() throws Exception {
        // data value is out bound of tAxis
        DateTime tValue = new DateTime(2000, 11, 02, 15, 00, chrnology);
        double zPos = 40.0;
        PlottingDomainParams params = new PlottingDomainParams(xSize, ySize,
                rGrid.getBoundingBox(), datasetZExtent, datasetTExtent, null, zPos, tValue);
        Collection<? extends DiscreteFeature<?, ?>> mapFeature = dataset.extractMapFeatures(null,
                params);
        assertEquals(0, mapFeature.size());

        // depth value is out bound of zAxis
        zPos = 140.0;
        tValue = new DateTime(2000, 01, 02, 15, 00, chrnology);
        params = new PlottingDomainParams(xSize, ySize, rGrid.getBoundingBox(), datasetZExtent,
                datasetTExtent, null, zPos, tValue);
        mapFeature = dataset.extractMapFeatures(null, params);
        assertEquals(0, mapFeature.size());
    }

    @Test
    public void testMapFeatures() throws Exception {
        DateTime tValue = new DateTime(2000, 01, 02, 15, 00, chrnology);
        for (Double zPos = 0.0; zPos <= 100; zPos += 10.0) {
            PlottingDomainParams params = new PlottingDomainParams(xSize, ySize,
                    rGrid.getBoundingBox(), datasetZExtent, datasetTExtent, null, zPos, tValue);
            Collection<? extends DiscreteFeature<?, ?>> mapFeature = dataset.extractMapFeatures(
                    null, params);
            assertEquals(1, mapFeature.size());

            DiscreteFeature<?, ?> feature = mapFeature.iterator().next();
            MapFeature data = (MapFeature) feature;

            Array2D<Number> lonValues = data.getValues("vLon");
            Array2D<Number> latValues = data.getValues("vLat");
            Array2D<Number> depthValues = data.getValues("vDepth");
            Array2D<Number> timeValues = data.getValues("vTime");

            assertArrayEquals(new int[] { ySize, xSize }, lonValues.getShape());
            assertArrayEquals(new int[] { ySize, xSize }, latValues.getShape());
            assertArrayEquals(new int[] { ySize, xSize }, depthValues.getShape());
            assertArrayEquals(new int[] { ySize, xSize }, timeValues.getShape());

            float expectedDepth = (float) zPos.doubleValue();

            for (int m = 0; m < ySize; m++) {
                float expectedLat = 100.0f * m / (ySize - 1);
                float expectedTime = 100.0f * tAxis.findIndexOf(tValue) / (tSize - 1);
                for (int n = 0; n < xSize; n++) {
                    float expectedLon = 100.0f * n / (xSize - 1);
                    assertEquals(expectedLon, lonValues.get(m, n).floatValue(), delta);
                    assertEquals(expectedLat, latValues.get(m, n).floatValue(), delta);
                    assertEquals(expectedDepth, depthValues.get(m, n).floatValue(), delta);
                    assertEquals(expectedTime, timeValues.get(m, n).floatValue(), delta);
                }
            }
        }
    }

    @Test
    public void testTimeSerieFeatures() throws Exception {
        for (Double zPos = 0.0; zPos <= 100; zPos += 10.0) {
            // to fetch data at one depth position, BBOX must set to null.
            // otherwise all values inside BBOX be returned as if targetZ
            // is ignored.

            PlottingDomainParams params = new PlottingDomainParams(xSize, ySize, null, null,
                    datasetTExtent, null, zPos, null);
            Collection<? extends PointSeriesFeature> timeSeriesFeatures = dataset
                    .extractTimeseriesFeatures(null, params);
            assertEquals(xSize * ySize, timeSeriesFeatures.size());

            for (PointSeriesFeature data : timeSeriesFeatures) {
                GridCoordinates2D gCoordinate = rGrid.findIndexOf(data.getHorizontalPosition());
                int xIndex = gCoordinate.getX();
                int yIndex = gCoordinate.getY();

                assertEquals(tAxis, data.getDomain());
                assertEquals(zPos, data.getVerticalPosition().getZ(), delta);

                Array1D<Number> lonValues = data.getValues("vLon");
                Array1D<Number> latValues = data.getValues("vLat");
                Array1D<Number> depthValues = data.getValues("vDepth");
                Array1D<Number> timeValues = data.getValues("vTime");

                assertArrayEquals(new int[] { tSize }, lonValues.getShape());
                assertArrayEquals(new int[] { tSize }, latValues.getShape());
                assertArrayEquals(new int[] { tSize }, depthValues.getShape());
                assertArrayEquals(new int[] { tSize }, timeValues.getShape());

                float expectedLon = 100.0f * xIndex / (xSize - 1);
                float expectedLat = 100.0f * yIndex / (ySize - 1);
                float expectedDepth = (float) zPos.doubleValue();

                for (int k = 0; k < tSize; k++) {
                    float expectedTime = 100 * k / 9.0f;
                    assertEquals(expectedLon, lonValues.get(k).floatValue(), delta);
                    assertEquals(expectedLat, latValues.get(k).floatValue(), delta);
                    assertEquals(expectedDepth, depthValues.get(k).floatValue(), delta);
                    assertEquals(expectedTime, timeValues.get(k).floatValue(), delta);
                }
            }
        }
    }

    
    //in this test, the params of zExtent and targetZ are set to various values to test 
    //the behaviours of extract TimesSeriesFeature method.
    @Test
    public void testTimeSerieFeaturesPartofZExents() throws Exception {
        //a zExtent is inside of the zExtent of dataset.
        
        Extent<Double> zExtent = Extents.newExtent(18.6, 53.4);
        PlottingDomainParams params = new PlottingDomainParams(xSize, ySize, null, zExtent,
                datasetTExtent, null, null, null);
        Collection<? extends PointSeriesFeature> timeSeriesFeatures = dataset
                .extractTimeseriesFeatures(null, params);
        for (PointSeriesFeature data : timeSeriesFeatures) {
            GridCoordinates2D gCoordinate = rGrid.findIndexOf(data.getHorizontalPosition());
            int xIndex = gCoordinate.getX();
            int yIndex = gCoordinate.getY();
            verifyTimeSeriesFeature(data, data.getHorizontalPosition(), xIndex, yIndex);
        }
        
        //a zExtent is overlay of the zExtent of dataset.
        zExtent = Extents.newExtent(20.0, 153.4);
        params = new PlottingDomainParams(xSize, ySize, null, zExtent,
                datasetTExtent, null, null, null);
        timeSeriesFeatures = dataset.extractTimeseriesFeatures(null, params);
        for (PointSeriesFeature data : timeSeriesFeatures) {
            GridCoordinates2D gCoordinate = rGrid.findIndexOf(data.getHorizontalPosition());
            int xIndex = gCoordinate.getX();
            int yIndex = gCoordinate.getY();
            verifyTimeSeriesFeature(data, data.getHorizontalPosition(), xIndex, yIndex);
        }
      
        //another zExtent is overlay of the zExtent of dataset.
        zExtent = Extents.newExtent(-120.6, 47.18);
        params = new PlottingDomainParams(xSize, ySize, null, zExtent,
                datasetTExtent, null, null, null);
        timeSeriesFeatures = dataset.extractTimeseriesFeatures(null, params);
        for (PointSeriesFeature data : timeSeriesFeatures) {
            GridCoordinates2D gCoordinate = rGrid.findIndexOf(data.getHorizontalPosition());
            int xIndex = gCoordinate.getX();
            int yIndex = gCoordinate.getY();
            verifyTimeSeriesFeature(data, data.getHorizontalPosition(), xIndex, yIndex);
        }

        //a zExtent is outside of the zExtent of dataset.
        zExtent = Extents.newExtent(-18.6, -10.0);
        params = new PlottingDomainParams(xSize, ySize, null, zExtent,
                datasetTExtent, null, null, null);
        timeSeriesFeatures = dataset.extractTimeseriesFeatures(null, params);
        assertEquals(0, timeSeriesFeatures.size());
        
        // a targetZ value is inside of the zExtent of the dataset
        double targetZ = 85.8;
        int zIndex = -1;
        if (vAxis.contains(targetZ)) {
            zIndex = vAxis.findIndexOf(targetZ);
            params = new PlottingDomainParams(xSize, ySize, null, null,
                    datasetTExtent, null, targetZ, null);
            timeSeriesFeatures = dataset.extractTimeseriesFeatures(null, params);

            for (PointSeriesFeature data : timeSeriesFeatures) {
                Array1D<Number> depthValues = data.getValues("vDepth");
                assertArrayEquals(new int[] { tSize }, depthValues.getShape());
                float expectedDepth = 10.0f * zIndex;
                for (int k = 0; k < tSize; k++) {
                    assertEquals(expectedDepth, depthValues.get(k).floatValue(), delta);
                }
            }
        }

        // a targetZ value is outside of the zExtent of the dataset
        targetZ = 110.8; // vAxis.contains(targetZ) return false
        params = new PlottingDomainParams(xSize, ySize, null, null,
                datasetTExtent, null, targetZ, null);
        timeSeriesFeatures = dataset.extractTimeseriesFeatures(null, params);
        assertEquals(0, timeSeriesFeatures.size());


        targetZ = -20.8; // vAxis.contains(targetZ) return false
        params = new PlottingDomainParams(xSize, ySize, null, null,
                datasetTExtent, null, targetZ, null);
        timeSeriesFeatures = dataset.extractTimeseriesFeatures(null, params);
        assertEquals(0, timeSeriesFeatures.size());
    }

    //in this test, the params of tExent and targetT are set to various
    //so the behaviours of extract TimeSeriesFeature method are tested.
    @Test
    public void testTimeSerieFeaturesPartofTExents() throws Exception {
        DateTime start = new DateTime(1999, 12, 25, 15, 00, chrnology);
        DateTime end = new DateTime(2000, 1, 8, 23, 00, chrnology);
        
        //a tExtent is intersected with the dataset tExent
        Extent<DateTime> tExtent = Extents.newExtent(start, end);
        extractTimeSeriesFeature(tExtent);

        //another tExtent is intersected with the dataset tExent
        start = new DateTime(2000, 1, 2, 00, 00, chrnology);
        end = new DateTime(2000, 1, 8, 00, 00, chrnology);
        tExtent = Extents.newExtent(start, end);
        extractTimeSeriesFeature(tExtent);

        // the tExtent is only one point
        start = new DateTime(2000, 1, 3, 15, 00, chrnology);
        end = new DateTime(2000, 1, 3, 15, 00, chrnology);
        tExtent = Extents.newExtent(start, end);
        extractTimeSeriesFeature(tExtent);

        //another tExtent is intersected with the dataset tExent
        start = new DateTime(2000, 1, 3, 15, 00, chrnology);
        end = new DateTime(2000, 5, 3, 15, 00, chrnology);
        tExtent = Extents.newExtent(start, end);
        extractTimeSeriesFeature(tExtent);

        //a tExtent is outside of the dataset tExent
        start = new DateTime(2000, 1, 20, 15, 00, chrnology);
        end = new DateTime(2000, 5, 3, 15, 00, chrnology);
        tExtent = Extents.newExtent(start, end);
        extractTimeSeriesFeature(tExtent);

        //another tExtent is outside of the dataset tExent
        start = new DateTime(1999, 10, 20, 15, 00, chrnology);
        end = new DateTime(1999, 12, 3, 23, 59, chrnology);
        tExtent = Extents.newExtent(start, end);
        extractTimeSeriesFeature(tExtent);
    }

    private void extractTimeSeriesFeature(Extent<DateTime> tExtent)
            throws Exception {
        PlottingDomainParams params = new PlottingDomainParams(xSize, ySize, null, datasetZExtent,
                tExtent, null, null, null);
        Collection<? extends PointSeriesFeature> timeSeriesFeatures = dataset
                .extractTimeseriesFeatures(null, params);
        if (tExtent == null || tExtent.intersects(datasetTExtent)) {
            assertEquals(zSize *xSize *ySize, timeSeriesFeatures.size());

            for (PointSeriesFeature feature : timeSeriesFeatures) {
                HorizontalPosition hPos =feature.getHorizontalPosition();
                GridCoordinates2D gCoordinate = rGrid.findIndexOf(hPos);;
                int xIndex = gCoordinate.getX();
                int yIndex = gCoordinate.getY();
                verifyTimeSeriesFeature(feature, hPos, xIndex, yIndex);
            }
        } else {
            assertEquals(0, timeSeriesFeatures.size());
        }
    }

    private void verifyTimeSeriesFeature(PointSeriesFeature data, HorizontalPosition hPos,
            int xIndex, int yIndex) {
        assertEquals(hPos, data.getHorizontalPosition());
        Array1D<Number> lonValues = data.getValues("vLon");
        Array1D<Number> latValues = data.getValues("vLat");
        Array1D<Number> depthValues = data.getValues("vDepth");
        Array1D<Number> timeValues = data.getValues("vTime");

        assertArrayEquals(new int[] { tSize }, lonValues.getShape());
        assertArrayEquals(new int[] { tSize }, latValues.getShape());
        assertArrayEquals(new int[] { tSize }, depthValues.getShape());
        assertArrayEquals(new int[] { tSize }, timeValues.getShape());

        float expectedLon = 100.0f * xIndex / (xSize - 1);
        float expectedLat = 100.0f * yIndex / (ySize - 1);
        float expectedDepth = (float) data.getVerticalPosition().getZ();

        for (int k = 0; k < tSize; k++) {
            float expectedTime = 100 * k / 9.0f;
            assertEquals(expectedLon, lonValues.get(k).floatValue(), delta);
            assertEquals(expectedLat, latValues.get(k).floatValue(), delta);
            assertEquals(expectedDepth, depthValues.get(k).floatValue(), delta);
            assertEquals(expectedTime, timeValues.get(k).floatValue(), delta);
        }
    }

    // General test to extract profile features
    @Test
    public void testProfileFeatures() throws Exception {
        DateTime dt = new DateTime(2000, 01, 05, 00, 00);

        Array<GridCell2D> cells = rGrid.getDomainObjects();
        for (GridCell2D cell : cells) {
            GridCoordinates2D gCoordinate = cell.getGridCoordinates();
            int xIndex = gCoordinate.getX();
            int yIndex = gCoordinate.getY();

            HorizontalPosition hPos = cell.getCentre();
            PlottingDomainParams params = new PlottingDomainParams(xSize, ySize, null,
                    datasetZExtent, null, hPos, null, dt);
            Collection<? extends ProfileFeature> profileFeature = dataset.extractProfileFeatures(
                    null, params);
            assertEquals(1, profileFeature.size());
            DiscreteFeature<?, ?> feature = profileFeature.iterator().next();
            assertTrue(feature instanceof ProfileFeature);
            verifyProfileFeature((ProfileFeature) feature, hPos, xIndex, yIndex);
        }
    }

    @Test
    public void testFeaturesBBox() throws Exception {
        BoundingBox bbox = new BoundingBoxImpl(-124.89, -20.9, 50.004, 25.0, crs);
        double xstep = (rGrid.getXAxis().getCoordinateExtent().getHigh() - rGrid.getXAxis()
                .getCoordinateExtent().getLow())
                / xSize;
        double ystep = (rGrid.getYAxis().getCoordinateExtent().getHigh() - rGrid.getYAxis()
                .getCoordinateExtent().getLow())
                / ySize;
        bboxTest(bbox, xstep, ystep);

        bbox = new BoundingBoxImpl(-134.9, -34.5, 0.8, -31.7, crs);
        bboxTest(bbox, xstep, ystep);

        bbox = new BoundingBoxImpl(-1000.0, -34.5, -759.334, 60.0, crs);
        bboxTest(bbox, xstep, ystep);

        bbox = new BoundingBoxImpl(-400.1, -0.3, 150.8, 58.0, crs);
        bboxTest(bbox, xstep, ystep);

        bbox = new BoundingBoxImpl(11.7, 12.0, 18.9, 70.0, crs);
        bboxTest(bbox, xstep, ystep);

        bbox = new BoundingBoxImpl(11.7, 105.9, 18.9, 190.0, crs);
        bboxTest(bbox, xstep, ystep);

        bbox = new BoundingBoxImpl(11.7, -200.9, 33.3, -140.0, crs);
        bboxTest(bbox, xstep, ystep);

        bbox = new BoundingBoxImpl(11.7, -200.9, 38.3, 10.0, crs);
        bboxTest(bbox, xstep, ystep);

        bbox = new BoundingBoxImpl(11.7, 30.8, 33.3, 150.0, crs);
        bboxTest(bbox, xstep, ystep);
    }

    private void bboxTest(BoundingBox bbox, double xstep, double ystep) throws Exception {
        PlottingDomainParams params = new PlottingDomainParams(xSize, ySize, bbox, null, null,
                null, null, null);
        Collection<? extends ProfileFeature> profileFeatures = dataset.extractProfileFeatures(null,
                params);

        params = new PlottingDomainParams(xSize, ySize, bbox, null, null, null, null, null);
        Collection<? extends PointSeriesFeature> timeSeriesFeatures = dataset
                .extractTimeseriesFeatures(null, params);

        // y-side need to be considered since x-side can be wrapped up
        Extent<Double> bboxY = Extents.newExtent(bbox.getMinY(), bbox.getMaxY());
        double yLow;
        double yHigh;
        if (bboxY.intersects(rGrid.getYAxis().getCoordinateExtent())) {
            double datasetYLow = rGrid.getYAxis().getCoordinateExtent().getLow();
            if (datasetYLow > bbox.getMinY())
                yLow = datasetYLow;
            else
                yLow = bbox.getMinY();
            double datasetYHigh = rGrid.getYAxis().getCoordinateExtent().getHigh();
            if (datasetYHigh > bbox.getMaxY())
                yHigh = bbox.getMaxY();
            else
                yHigh = datasetYHigh;
            int numberOfYMidPoints = getNumberOfMidPoints(yLow, yHigh, ystep);
            if (numberOfYMidPoints < 0) {
                assertEquals(0, timeSeriesFeatures.size());
                assertEquals(0, profileFeatures.size());
            } else {
                int numberOfXMidPoints = getNumberOfMidPoints(bbox.getMinX(), bbox.getMaxX(), xstep);
                if (numberOfXMidPoints < 0) {
                    assertEquals(0, timeSeriesFeatures.size());
                    assertEquals(0, profileFeatures.size());
                } else {
                    assertEquals(numberOfXMidPoints * numberOfYMidPoints * zSize,
                            timeSeriesFeatures.size());
                    assertEquals(numberOfXMidPoints * numberOfYMidPoints * tSize,
                            profileFeatures.size());
                }
            }
        }
    }

    private int getNumberOfMidPoints(double low, double high, double step) {
        if (high - low > 360.0)
            return (int) (360.0 / step);
        int m = (int) Math.ceil(low / step);
        int n = (int) Math.floor(high / step);
        if (n < m)
            return -1;
        else
            return n - m + 1;
    }

    // for profile features, targetZ is ignored.
    private void zExtentCaseForProfileFeatures(Extent<Double> zExtent, GridCell2D cell)
            throws Exception {
        HorizontalPosition hPos = cell.getCentre();
        PlottingDomainParams params = new PlottingDomainParams(xSize, ySize, null, zExtent, null,
                hPos, null, null);
        Collection<? extends ProfileFeature> profileFeature = dataset.extractProfileFeatures(null,
                params);
        if (zExtent == null || zExtent.intersects(datasetZExtent)) {
            assertEquals(tSize, profileFeature.size());
            GridCoordinates2D gCoordinate = cell.getGridCoordinates();
            int xIndex = gCoordinate.getX();
            int yIndex = gCoordinate.getY();
            for (ProfileFeature feature : profileFeature) {
                verifyProfileFeature(feature, hPos, xIndex, yIndex);
            }
        } else {
            assertEquals(0, profileFeature.size());
        }
    }

    @Test
    public void testProfileFeaturesPartOfZExtent() throws Exception {
        Array<GridCell2D> cells = rGrid.getDomainObjects();

        // test case 1
        Extent<Double> zExtent = Extents.newExtent(1220.0, 4401.0);
        for (GridCell2D cell : cells) {
            zExtentCaseForProfileFeatures(zExtent, cell);
        }

        // test case 2, the given zExtent intersect with the one of the data set
        // it should return all profile features the data set contain
        zExtent = Extents.newExtent(25.0, 300.0);
        for (GridCell2D cell : cells) {
            zExtentCaseForProfileFeatures(zExtent, cell);
        }

        // test case 3: a given zExtent is in the range of of the zExtent of the
        // dataset
        // all profile features return
        zExtent = Extents.newExtent(38.0, 55.0);
        for (GridCell2D cell : cells) {
            zExtentCaseForProfileFeatures(zExtent, cell);
        }

        // test case 4: another example a given zExtent intersects with the
        // zExtent of the data set.
        zExtent = Extents.newExtent(-100.0, 95.0);
        for (GridCell2D cell : cells) {
            zExtentCaseForProfileFeatures(zExtent, cell);
        }

        zExtent = Extents.newExtent(-100.0, -5.0);
        for (GridCell2D cell : cells) {
            zExtentCaseForProfileFeatures(zExtent, cell);
        }

        for (GridCell2D cell : cells) {
            zExtentCaseForProfileFeatures(null, cell);
        }
    }

    //set the params of tExent and targetT for various values
    //in order to test extractProfileFeature method
    @Test
    public void testProfileFeaturesPartOfTExtent() throws Exception {
        DateTime start = new DateTime(2000, 01, 01, 00, 00, chrnology);
        DateTime end = start;
        Extent<DateTime> tExtent = Extents.newExtent(start, end);
        DateTime targetT =null;
        
        //tExent is one point, targetT is null
        tExentCaseForProfileFeatures(tExtent, null);

        // a tExent is overlay (left side) of the dataset tExent
        tExtent = Extents.newExtent(start.minusDays(100), end.plusDays(5));
        tExentCaseForProfileFeatures(tExtent, null);

        // a tExent includes the whole dataset tExent
        tExtent = Extents.newExtent(start.minusDays(100), end.plusDays(50));
        tExentCaseForProfileFeatures(tExtent, null);

        // a tExent is overlay (right side) of the dataset tExent
        tExtent = Extents.newExtent(start.plusDays(3), end.plusDays(500));
        tExentCaseForProfileFeatures(tExtent, null);

        //a targetT is exactly on the tAxis point
        targetT = new DateTime(2000, 01, 05, 00, 00, chrnology);
        tExentCaseForProfileFeatures(tExtent, targetT);
      
        //a targetT is on the tAxis 
        targetT = new DateTime(2000, 01, 05, 10, 50, chrnology);
        tExentCaseForProfileFeatures(tExtent, targetT);

        // a targetT is outside of the tAxis
        targetT = new DateTime(1999, 01, 05, 10, 50, chrnology);
        tExentCaseForProfileFeatures(tExtent, targetT);
    }

    private void tExentCaseForProfileFeatures(Extent<DateTime> tExtent, DateTime targetT) throws Exception {
        PlottingDomainParams params = new PlottingDomainParams(xSize, ySize, null, datasetZExtent,
                tExtent, null, null, targetT);
        Collection<? extends ProfileFeature> profileFeatures = dataset.extractProfileFeatures(null,
                params);
        if (tExtent == null && targetT == null) {
            assertEquals(tSize *xSize *ySize, profileFeatures.size());
            for (ProfileFeature feature : profileFeatures) {
                HorizontalPosition hPos =feature.getHorizontalPosition();
                GridCoordinates2D gCoordinate = rGrid.findIndexOf(hPos);
                int xIndex = gCoordinate.getX();
                int yIndex = gCoordinate.getY();
                verifyProfileFeature(feature, hPos, xIndex, yIndex);
            }
        } else if (tExtent == null && targetT != null) {
            if (tAxis.contains(targetT)) {
                assertEquals(xSize *ySize, profileFeatures.size());
                for (ProfileFeature feature : profileFeatures) {
                    HorizontalPosition hPos =feature.getHorizontalPosition();
                    GridCoordinates2D gCoordinate = rGrid.findIndexOf(hPos);
                    int xIndex = gCoordinate.getX();
                    int yIndex = gCoordinate.getY();
                    verifyProfileFeature(feature, hPos, xIndex, yIndex);
                }
            } else {
                assertEquals(0, profileFeatures.size());
            }
        } else if (tExtent != null && tExtent.intersects(datasetTExtent)) {
            for (ProfileFeature feature : profileFeatures) {
                HorizontalPosition hPos =feature.getHorizontalPosition();
                GridCoordinates2D gCoordinate = rGrid.findIndexOf(hPos);
                int xIndex = gCoordinate.getX();
                int yIndex = gCoordinate.getY();
                verifyProfileFeature(feature, hPos, xIndex, yIndex);
            }
        } else if (tExtent != null && !tExtent.intersects(datasetTExtent)) {
            assertEquals(0, profileFeatures.size());
        }
    }

    private void verifyProfileFeature(ProfileFeature data, HorizontalPosition hPos, int xIndex,
            int yIndex) {
        Array1D<Number> lonValues = data.getValues("vLon");
        Array1D<Number> latValues = data.getValues("vLat");
        Array1D<Number> depthValues = data.getValues("vDepth");
        Array1D<Number> timeValues = data.getValues("vTime");

        assertArrayEquals(new int[] { zSize }, latValues.getShape());
        assertArrayEquals(new int[] { zSize }, depthValues.getShape());

        float expectedLon = 100.0f * xIndex / (xSize - 1);
        float expectedLat = 100.0f * yIndex / (ySize - 1);
        int dateIndex = tAxis.findIndexOf(data.getTime());
        float expectedTime = 100 * dateIndex / 9.0f;

        for (int k = 0; k < zSize; k++) {
            float expectedDepth = 10.0f * k;
            assertEquals(expectedLon, lonValues.get(k).floatValue(), delta);
            assertEquals(expectedLat, latValues.get(k).floatValue(), delta);
            assertEquals(expectedDepth, depthValues.get(k).floatValue(), delta);
            assertEquals(expectedTime, timeValues.get(k).floatValue(), delta);
        }
    }

    @Test
    public void testNullProfileFeatures() throws Exception {
        DateTime start = new DateTime(2011, 01, 15, 00, 00, chrnology);
        DateTime end = new DateTime(2012, 02, 03, 00, 00, chrnology);
        Extent<DateTime> tExtent = Extents.newExtent(start, end);
        PlottingDomainParams params = new PlottingDomainParams(xSize, ySize, null,
                datasetZExtent, tExtent, null, null, null);
        Collection<? extends ProfileFeature> profileFeatures = dataset.extractProfileFeatures(
                null, params);
        assertEquals(0, profileFeatures.size());

        start = new DateTime(1990, 01, 15, 00, 00, chrnology);
        end = new DateTime(1998, 02, 03, 00, 00, chrnology);
        tExtent = Extents.newExtent(start, end);
        params = new PlottingDomainParams(xSize, ySize, null,
                datasetZExtent, tExtent, null, null, null);
        profileFeatures = dataset.extractProfileFeatures(null, params);
        assertEquals(0, profileFeatures.size());

        start = new DateTime(2000, 1, 1, 00, 00, chrnology);
        end = new DateTime(2000, 1, 10, 00, 00, chrnology);
        tExtent = Extents.newExtent(start, end);
        Extent<Double> zExtent = Extents.newExtent(200.0, 500.0);
        params = new PlottingDomainParams(xSize, ySize, null,
                zExtent, tExtent, null, null, null);
        profileFeatures = dataset.extractProfileFeatures(null, params);
        assertEquals(0, profileFeatures.size());

        start = new DateTime(2000, 1, 1, 00, 00, chrnology);
        end = new DateTime(2000, 1, 10, 00, 00, chrnology);
        tExtent = Extents.newExtent(start, end);
        zExtent = Extents.newExtent(-200.0, -50.0);
        params = new PlottingDomainParams(xSize, ySize, null,
                zExtent, tExtent, null, null, null);
        profileFeatures = dataset.extractProfileFeatures(null, params);
        assertEquals(0, profileFeatures.size());
    }

    @Test
    public void readFeatureTest() throws Exception {
        assertTrue(dataset.readFeature("vLon") instanceof GridFeature);
        assertFalse(dataset.readFeature("vLat") instanceof PointSeriesFeature);
        assertFalse(dataset.readFeature("vDepth") instanceof ProfileFeature);
        assertFalse(dataset.readFeature("vTime") instanceof TrajectoryFeature);
        assertFalse(dataset.readFeature("vLat") instanceof MapFeature);

        GridFeature feature = (GridFeature) dataset.readFeature("vLon");
        Array4D<Number> values = feature.getValues("vLon");
        int Tsize = values.getTSize();
        int Zsize = values.getZSize();
        int Ysize = values.getYSize();
        int Xsize = values.getXSize();

        for (int i = 0; i < Tsize; i++) {
            for (int j = 0; j < Zsize; j++) {
                for (int k = 0; k < Ysize; k++) {
                    for (int m = 0; m < Xsize; m++) {
                        Number vLonValue = values.get(i, j, k, m);
                        float expectedValue = 100.0f * m / (xSize - 1);
                        assertEquals(expectedValue, vLonValue.floatValue(), delta);
                    }
                }
            }
        }

        // The following test MapFeaure interface
        Set<String> ids = new TreeSet<>();
        ids.add("vLon");

        // T and Z values are fixed
        double zValue = 60.0;
        DateTime tValue = new DateTime(2000, 01, 01, 00, 00, chrnology);
        MapDomain mapdomain = new MapDomainImpl(rGrid, zValue, vCrs, tValue);

        MapFeature mapfeature = feature.extractMapFeature(ids, rGrid, zValue, tValue);
        assertEquals(mapdomain, mapfeature.getDomain());
        assertEquals(zValue, mapfeature.getDomain().getZ().doubleValue(), delta);
        assertEquals(tValue, mapfeature.getDomain().getTime());

        for (int k = 0; k < Ysize; k++) {
            for (int m = 0; m < Xsize; m++) {
                float expectedLonValue = 100.0f * m / (xSize - 1);
                assertEquals(expectedLonValue, mapfeature.getValues("vLon").get(k, m).floatValue(),
                        delta);
            }
        }
    }
}
