/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Requests";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "Executions";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "Response Times (ms)";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Throughput";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "Network (KB/sec)";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 99.4466767670114, "KoPercent": 0.5533232329886042};
    var dataset = [
        {
            "label" : "FAIL",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "PASS",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.994335024043212, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [1.0, 500, 1500, "S1 - Para yatır (POST deposit)"], "isController": false}, {"data": [1.0, 500, 1500, "setUp - Transfer hesabı B aç (5000 TL)"], "isController": false}, {"data": [0.9998401023345059, 500, 1500, "S1 - Bakiye sorgula (GET account)"], "isController": false}, {"data": [0.9615674453654861, 500, 1500, "S2 - Transfer A -> B"], "isController": false}, {"data": [0.9749430523917996, 500, 1500, "S2 - Transfer B -> A"], "isController": false}, {"data": [1.0, 500, 1500, "tearDown - Yatırma hesabı son bakiye"], "isController": false}, {"data": [0.0, 500, 1500, "tearDown - Yatırma hesabı işlem geçmişi"], "isController": false}, {"data": [1.0, 500, 1500, "setUp - Yatırma hesabı aç (PREMIUM, 0 TL)"], "isController": false}, {"data": [1.0, 500, 1500, "tearDown - Transfer hesabı A"], "isController": false}, {"data": [1.0, 500, 1500, "setUp - Transfer hesabı A aç (5000 TL)"], "isController": false}, {"data": [1.0, 500, 1500, "tearDown - Transfer hesabı B"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 15181, 84, 0.5533232329886042, 120.0699558658853, 88, 1648, 107.0, 154.0, 181.0, 273.4400000000023, 161.48969214730974, 151.00090644147713, 32.25596138810289], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["S1 - Para yatır (POST deposit)", 6276, 0, 0.0, 124.34018483110279, 91, 482, 109.0, 165.0, 195.0, 318.6899999999987, 104.55817672930829, 92.02456712627449, 21.851025214914035], "isController": false}, {"data": ["setUp - Transfer hesabı B aç (5000 TL)", 1, 0, 0.0, 107.0, 107, 107, 107.0, 107.0, 107.0, 107.0, 9.345794392523365, 8.314471378504672, 2.3912091121495327], "isController": false}, {"data": ["S1 - Bakiye sorgula (GET account)", 6254, 0, 0.0, 118.88951071314348, 89, 568, 106.0, 152.0, 179.0, 261.0, 104.79573712255772, 91.93313687099936, 17.602408969804618], "isController": false}, {"data": ["S2 - Transfer A -> B", 1327, 51, 3.8432554634513942, 112.79728711379045, 88, 393, 104.0, 146.0, 156.0, 195.0400000000002, 44.22743634182109, 34.25135763148247, 11.186431652862952], "isController": false}, {"data": ["S2 - Transfer B -> A", 1317, 33, 2.5056947608200457, 111.23082763857255, 91, 495, 104.0, 144.0, 152.0, 179.91999999999962, 44.35388812177955, 34.29532397155895, 11.218415062051662], "isController": false}, {"data": ["tearDown - Yatırma hesabı son bakiye", 1, 0, 0.0, 159.0, 159, 159, 159.0, 159.0, 159.0, 159.0, 6.289308176100629, 5.50314465408805, 1.0564072327044025], "isController": false}, {"data": ["tearDown - Yatırma hesabı işlem geçmişi", 1, 0, 0.0, 1648.0, 1648, 1648, 1648.0, 1648.0, 1648.0, 1648.0, 0.6067961165048543, 687.8656183631675, 0.10962625151699029], "isController": false}, {"data": ["setUp - Yatırma hesabı aç (PREMIUM, 0 TL)", 1, 0, 0.0, 492.0, 492, 492, 492.0, 492.0, 492.0, 492.0, 2.032520325203252, 1.8121982977642277, 0.5061451981707317], "isController": false}, {"data": ["tearDown - Transfer hesabı A", 1, 0, 0.0, 103.0, 103, 103, 103.0, 103.0, 103.0, 103.0, 9.70873786407767, 8.533070388349515, 1.6307645631067962], "isController": false}, {"data": ["setUp - Transfer hesabı A aç (5000 TL)", 1, 0, 0.0, 103.0, 103, 103, 103.0, 103.0, 103.0, 103.0, 9.70873786407767, 8.694250606796118, 2.4840716019417477], "isController": false}, {"data": ["tearDown - Transfer hesabı B", 1, 0, 0.0, 103.0, 103, 103, 103.0, 103.0, 103.0, 103.0, 9.70873786407767, 8.552032766990292, 1.6307645631067962], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["500/Internal Server Error", 84, 100.0, 0.5533232329886042], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 15181, 84, "500/Internal Server Error", 84, "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["S2 - Transfer A -> B", 1327, 51, "500/Internal Server Error", 51, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["S2 - Transfer B -> A", 1317, 33, "500/Internal Server Error", 33, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
