function getStart()
{
    var randX = getRand(-20,20)
    var randY = getRand(5,10)
    var randZ = getRand(-20,20)
    var arr = [randX,1,randZ]
    return arr;
}

function getRand(min, max)
{
    var randomNumber = Math.floor(Math.random() * (max - min + 1)) + min;
    return(10);
}