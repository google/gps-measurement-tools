
http://www.mathworks.com/matlabcentral/fileexchange/27627-zoharby-plot-google-map

Description 
plot_google_map.m uses the Google Maps API to plot a map in the background of the current figure. 
It assumes the coordinates of the current figure are in the WGS84 datum, and uses a conversion code to convert and project the image from the coordinate system used by Google into WGS84 coordinates. 
The zoom level of the map is automatically determined to cover the entire area of the figure. Additionally, it has the option to auto-refresh the map upon zooming in the figure, revealing more details as one zooms in. 
The following code produces the screenshot:

lat = [48.8708 51.5188 41.9260 40.4312 52.523 37.982]; 
lon = [2.4131 -0.1300 12.4951 -3.6788 13.415 23.715]; 
plot(lon,lat,'.r','MarkerSize',20) 
plot_google_map

Known Issues: 
1) The static maps API is limited to 1000 requests per day when used with no API key. If you use this function a lot, you can obtain an API key and easily set the function to use it (see help for more details) 
2) Saving the map with an image/matrix overlay drawn on top of it (especially a semi-transparent one) can sometimes cause unexpected results (map not showing etc.). If you're encountering such problems, it's recommended to use the export_fig submission: 
http://www.mathworks.com/matlabcentral/fileexchange/23629-exportfig 
The combination that seems to work best: 
set(gcf,'renderer','zbuffer') 
export_fig('out.jpg')
 
Acknowledgements 
Get Google Map inspired this file.

This file inspired Visual Inertial Odometry, Jc Dstatus Plot, Landsat, Borders, and Gps2 Cart.
 
MATLAB release MATLAB 7.9 (R2009b)  
