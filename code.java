var region = geometry;
var start='2020-01-01';
var end='2020-06-01';


var collection = ee.ImageCollection('LANDSAT/LC08/C01/T1_TOA').filterDate(start, end).filterBounds(region). map(addQualityBands);
var recentValueComposite = collection.qualityMosaic('system:time_start');
var greenestPixelComposite = collection.qualityMosaic('nd');
var greenestPixelComposite = greenestPixelComposite.select(['B1','B2','B3','B4','B5','B6','B7']);
Map.centerObject(region, 11);
Map.addLayer(greenestPixelComposite, {bands: 'B5,B6,B4', min:0.0,  max: [0.4, 0.3, 0.2]}, '564 Landsat composite');
Export.image.toDrive({
  image: greenestPixelComposite, // image to be exported
  description: 'Landsat8_composite',
  fileNamePrefix: 'Landsat8_composite', // output name
  scale: 30, // pixel size
  region: region // area to export
});


// ------------------ Function to mask clouds using the quality band of Landsat 8---------------------------
var maskL8 = function(image) {
  var cloudShadowBitMask = (1 << 3);
  var cloudsBitMask = (1 << 5);
  var qa = image.select('pixel_qa').clip(area);
  /// Check that the cloud bit is off
  var mask = qa.bitwiseAnd(cloudShadowBitMask).eq(0)
      .and(qa.bitwiseAnd(cloudsBitMask).eq(0));
  return image.updateMask(mask);
};


//------------------ Calculate MSAVI ---------------------------

// MSAVI = (2 * NIR + 1 – sqrt ((2 * NIR + 1)2 – 8 * (NIR - R))) / 2

var msavi = composite.expression(
  '(2 * NIR + 1 - sqrt(pow((2 * NIR + 1), 2) - 8 * (NIR - RED)) ) / 2', 
  {
    'NIR': composite.select('B8'), 
    'RED': composite.select('B4')
  }
);

Map.addLayer(msavi, {min: -0.1, max: 0.5}, 'msavi2 (expression)')

// ------------------ Compute MSAVI  ---------------------------

msavi = composite.select('B8').multiply(2).add(1)
  .subtract(composite.select('B8').multiply(2).add(1).pow(2)
    .subtract(composite.select('B8').subtract(composite.select('B4')).multiply(8)).sqrt()
  ).divide(2)

Map.addLayer(msavi, {min: -0.1, max: 0.5}, 'msavi (fluent)')
