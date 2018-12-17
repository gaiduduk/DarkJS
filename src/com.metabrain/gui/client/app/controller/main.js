app.controller("main", function ($scope, $mdDialog) {

    let width = 700,
        height = 500;

    let centerPos = [width / 2, height / 2];

    var nodeRadius = 20;

    let startTime;

    var currentLink = N + 0;

    function nozoom() {
        d3.event.preventDefault();
    }

    let svg = d3.select("#canvas")
        .on("touchstart", nozoom)
        .on("touchmove", nozoom)
        .append("svg")
        .attr("width", width)
        .attr("height", height);

    let root = svg.append("g")
        .call(d3.behavior.zoom()
            .on("zoom", function () {
                view.attr("transform", tr(posSum(centerPos, d3.event.translate), d3.event.scale));
            }));

    root.append("rect")
        .attr("width", width)
        .attr("height", height)
        .attr("fill", "white")
        .on("mousedown", function () {
            startTime = new Date();
            hideMenu();
        })
        .on("click", function clicked(d, i) {
            if (d3.event.defaultPrevented) return; // zoomed

            if (new Date() - startTime > 300) {
                let pos = d3.mouse(view.node());
                createLocalNode(currentLink, function (link) {
                    setStyle(link, {
                        x: pos[0],
                        y: pos[1],
                        r: nodeRadius,
                    }, function () {
                        showNode(currentLink)
                    })
                });
            }
            d3.select(this).transition()
                .style("fill", "black")
                .transition()
                .style("fill", "white");
        });


    let view = root.append("g")
        .attr("class", "circles")
        .attr("transform", tr(width / 2, height / 2));

    var resizeBtn = view.append("circle")
        .attr("class", "resize")
        .style("opacity", 0)
        .attr("r", nodeRadius)

    function showMenu(ths) {
        var translate = tr(posSum(getTranslate(ths), [0, -nodeRadius * 2 - 10]));
        resizeBtn.attr("transform", translate)
            .transition()
            .duration(300)
            .style("opacity", 1);
    }

    function hideMenu() {
        if (resizeBtn.attr("opacity") !== 0)
            resizeBtn.transition()
                .duration(300)
                .style("opacity", 0)
    }


    let centerOffset;
    var startDragPos;

    var showNode = function (link) {
        currentLink = link;
        let showNode = nodes[currentLink];
        view.selectAll(".node").remove();
        let nodeList = view.selectAll(".node")
            .data(showNode.local || []).enter()
            .append("g")
            .attr("class", "node")
            .attr("transform", function (link) {
                var x = getStyleValue(link, "x", 0);
                var y = getStyleValue(link, "y", 0);
                return tr(x, y);
            })
            .on("dblclick", function (link) {
                d3.event.stopPropagation();
                openCodeEditor($mdDialog, $scope.$new(), currentLink, link)
            })
            .call(d3.behavior.drag()
                .origin(function (d) {
                    return d;
                })
                .on("dragstart", function (d) {
                    hideMenu();
                    startTime = new Date();
                    d3.event.sourceEvent.stopPropagation();
                    startDragPos = getTranslate(this);
                    centerOffset = d3.mouse(this);
                    d3.select(this).classed("dragging", true);
                })
                .on("drag", function () {
                    var translate = tr(
                        posSum(
                            posSub(getTranslate(this), centerOffset),
                            d3.mouse(this)));
                    d3.select(this).attr("transform", translate);
                })
                .on("dragend", function (link) {
                    var translate = getTranslate(this);
                    let node = d3.select(this);
                    node.classed("dragging", false);
                    if (new Date() - startTime > 300 //long click{
                        && posDst(startDragPos, translate) < 20) {
                        showMenu(this);
                    }
                    setStyle(link, {
                        x: translate[0],
                        y: translate[1],
                    }, function () {
                        showNode(currentLink)
                    });
                }));

        nodeList.append("circle")
            .attr("r", function (link) {
                return getStyleValue(link, "r", 20);
            });
        nodeList.append("text")
            .attr("dx", nodeRadius + 10)
            .text(function (link) {
                return getTitle(link)
            });

        var tt = tr(getTranslate(view.node()), 1.0);
        view.attr("transform", tt)
            .transition().duration(1000)
            .style("opacity", 1)
    };


    loadNode(currentLink, showNode);

    function toolbarAnimation() {
        var r = 10000;
        var title2 = root.append("path")
            .attr("class", "title2")
            .attr("fill", "red");
        root.append("path")
            .attr("class", "title1")
            .attr("d", d3.svg.arc()
                .innerRadius(r)
                .outerRadius(r + 1000)
                .startAngle(rad(0))
                .endAngle(rad(360)))
            .attr("transform", tr(width / 2, r + 40))
            .on("click", function () {
                d3.select(this).transition().duration(1000)
                    .attr("d", d3.svg.arc()
                        .innerRadius(0.01)
                        .outerRadius(nodeRadius)
                        .startAngle(rad(0))
                        .endAngle(rad(360)))
                    .attr("transform", tr(width / 2, height / 2))
                    .each("end", function () {
                        d3.select(this).transition().duration(1000)
                            .style("opacity", 0)
                            .each("end", function () {
                                d3.select(this)
                                    .style("opacity", 1)
                                    .attr("d", d3.svg.arc()
                                        .innerRadius(r)
                                        .outerRadius(r + 1000)
                                        .startAngle(rad(0))
                                        .endAngle(rad(360)))
                                    .attr("transform", tr(width / 2, r + 40));
                            });

                        title2.transition()
                            .delay(500)
                            .duration(1000)
                            .style("opacity", 1);

                    });

                title2
                    .attr("d", d3.svg.arc()
                        .innerRadius(r)
                        .outerRadius(r + 1000)
                        .startAngle(rad(0))
                        .endAngle(rad(360)))
                    .attr("transform", tr(width / 2, r))
                    .transition()
                    .duration(1000)
                    .attr("transform", tr(width / 2, r + 40));

                view.transition().duration(1000)
                    .attr("transform", tr(getTranslate(view.node()), 0.001))
                    .style("opacity", 0)
                    .each("end", function () {
                        showNode(currentLink);
                    })
            });

    }

    toolbarAnimation();
});