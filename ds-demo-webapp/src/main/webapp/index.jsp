<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page import="ezbake.configuration.EzConfiguration,ezbake.security.client.EzbakeSecurityClient" %>
<!--
 * Copyright (C) 2013-2014 Computer Sciences Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. -->
<html>
<head>

    <!-- Bootstrap -->
    <link href="css/bootstrap.css" rel="stylesheet">
    <!--<link href="css/bootstrap.css.map" rel="stylesheet">-->
    <!--<link href="css/bootstrap-theme.css.map" rel="stylesheet">-->
    <link href="css/bootstrap-theme.css" rel="stylesheet">

    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
    <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->

    <script src="jquery-1.10.2.js"></script>

    <script type="text/javascript">
        $(function () {
            $('#inputContent').keyup(function () {
                var count = $(this).val().length;
                $('#characters').text(count);
                //console.log("# char: " + count);
            });

            $("#Search").keyup(function (event) {
                //console.log("searchText keyup: " + event.keyCode);
                if (event.keyCode == 13) {
                    $("#searchButton").click();
                }
            });
        });

        function insert() {
            if (!validateNumCharacters()) {
                return;
            }

            var content = $("#inputContent").val();
            var dataset = $(".dataset:checked").val();
            var visibility = $("#inputVisibility").val();
            var group =  $(".ezgroup:checked").val();

            $("#insertResult").attr('class', 'text-success');
            $("#insertResult").html("Calling the dataset service, please wait...");
            $("#insertButton").prop("disabled", true);

            $.ajax({
                url: 'DatasetServlet', type: "POST", data: {
                    action: 'insertText', content: content, dataset: dataset, visibility: visibility,
                    group: group
                }, success: function (result) {
                    $("#insertResult").attr('class', 'text-success');
                    $("#insertResult").html(result);
                    $("#insertButton").prop("disabled", false);
                }, error: function (xhr, status, result) {
                    var err = xhr.responseText;
                    console.log("ERROR in insert: " + err);
                    $("#insertResult").attr('class', 'text-danger');
                    $("#insertResult").html(err);
                    $("#insertButton").prop("disabled", false);
                }
            });
        }


        function search() {
            var searchText = $("#searchText").val();
            var dataset = $(".dataset:checked").val();
            var group =  $(".ezgroup:checked").val();

            $.ajax({
                url: 'DatasetServlet', type: "POST", data: {
                    action: 'search', searchText: searchText, dataset: dataset,
                    group: group
                }, success: function (result) {
                    $("#searchResult tbody").html(result);
                }, error: function (xhr, status, result) {
                    var err = xhr.responseText;
                    console.log("ERROR in search: " + err);
                    $("#searchResult tbody").html(err);
                }
            });

        }

        function validateNumCharacters() {
            var count = $('#inputContent').val().length;

            if (count > 140) {
                alert("You have exceeded 140 characters: " + count);
                return false;
            }

            return true;
        }
    </script>
</head>
<body>

<div id="auths" style="background-color: gold">
    <% try {
        EzbakeSecurityClient securityClient = new EzbakeSecurityClient(new EzConfiguration().getProperties());
        out.println(securityClient.fetchTokenForProxiedUser().getTokenPrincipal().getPrincipal());
        out.println(securityClient.fetchTokenForProxiedUser().getAuthorizations());
    } catch (Exception e) {
        e.printStackTrace();
    }%>
</div>
<div id="warning" style="background-color: red; text-align:center; color:white">
    Please note that the security level visibilities in this app are all test data - not real classified data.
</div>

<div id="container">
    <div class="panel panel-default" id="content">
        <h3 class="panel-title">Datasets Webapp Demo</h3>

        <!-- Insert Text -->
        <div class="panel-body">

            <h3>Select Your Favorite Dataset:</h3>
            <p><input id="dataset" type="radio" name="dataset" class="dataset" value="mongo"> Mongo</input></p>
            <p><input id="dataset" type="radio" name="dataset" class="dataset" value="elastic" checked> Elastic</input></p>
            <p><input id="dataset" type="radio" name="dataset" class="dataset" value="postgres"> Postgres</input></p>

            <h3>Select Your Favorite Group:</h3>
            <p><input id="ezgroup" type="radio" name="ezgroup" class="ezgroup" value="none" checked> NONE</input></p>
            <p><input id="ezgroup" type="radio" name="ezgroup" class="ezgroup" value="demo-group-a"> A</input></p>
            <p><input id="ezgroup" type="radio" name="ezgroup" class="ezgroup" value="demo-group-b"> B</input></p>

            <label>Insert Text</label>

            <textarea id="inputContent" class="form-control" placeholder="Enter text here"></textarea>
            <em class="hint" style="margin-top: 10px;">Number of characters:
                <div id="characters" class="numCharacters">0</div>
            </em>
            <em class="hint" style="margin-top: 10px;">(max 140 characters)</em>
            <input id="inputVisibility" type="text" class="form-control" placeholder="Enter visibility. Default is 'U' (Unclassified)"></br>
            <span class="input-group-btn">
                <button class="btn btn-info btn-large" id="insertButton" type="button" name="insert"
                        onClick="insert()">Submit
                </button>
            </span>

            <!-- Insert output msg -->
            <div id="insertResult" class="text-success"></div>
        </div>

        <!-- Search dataset -->
        <ul class="list-group">
            <li class="list-group-item">
                <label>Search Text</label>
                <em class="hint" style="margin-top: 10px;">Enter search term :</em>
                <input id="searchText" type="text" class="form-control" placeholder="Enter search term here"></br>

            <span class="input-group-btn">
                <button class="btn btn-info btn-large" id="searchButton" type="button" name="search"
                        onClick="search()">Search
                </button>
            </span>
            </li>
            <li class="list-group-item">
                <!-- Search result output list -->
                <table id="searchResult" class="table table-condensed table-bordered table-hover table-striped">
                    <thead>
                        <tr>
                            <th class="text">Result</th>
                        </tr>
                    </thead>
                    <tbody>
                        <!-- empty on initial page load. Will get populated by search results using AJAX. -->
                    </tbody>
                </table>
            </li>
        </ul>
    </div>
</div>

<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
<!-- Include all compiled plugins (below), or include individual files as needed -->
<script src="js/bootstrap.min.js"></script>
</body>
</html>
