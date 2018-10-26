<!DOCTYPE html>
<html lang="en">
<head>
    <title>Bootstrap Example</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
    <style>
        .fakeimg {
            height: 200px;
            background: #aaa;
        }
    </style>
</head>
<body>

<div class="jumbotron text-center" style="margin-bottom:0">
    <h1>S2 <small>tool</small></h1>
    <p>WEB part of CLI</p>
</div>

<?php echo 4;?>

<div class="container">
    <ul class="nav nav-tabs bg-dark ">
        <li class="active"><a data-toggle="tab" href="#home">Home</a></li>
        <li><a data-toggle="tab" href="#modify">Modify</a></li>
        <li><a data-toggle="tab" href="#generate">Generate</a></li>
        <li><a data-toggle="tab" href="#documentation">Documentation</a></li>
    </ul>

    <div class="tab-content">
        <div id="home" class="tab-pane fade in active">
            <h3>README</h3>
            <p>Web interfance off CLI project</p>
            <h3>DEPENDENCIES</h3>
            <blockquote>Finding prooper dependencies is left for the reader as an exercise!!!
                <footer>lazy developer</footer>
            </blockquote>
            <h3>LICENSE THEOREM</h3>
            <p>There exist such license that this project is suitable to that exact license.</p>

        </div>
        <div id="modify" class="tab-pane fade">

            <button type="button" class="btn btn-primary">Primary</button>
            <h3>Menu 1</h3>
            <p>Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>
            <form method="post" enctype="multipart/form-data">
                <input type="file" name="files[]" multiple>
                <input type="submit" value="Upload File" name="submit">
            </form>
        </div>
        <div id="generate" class="tab-pane fade">
            <h3>Menu 2</h3>
            <p><?php echo date('Y');?></p>
            <p>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam.</p>
            <img src="../WEB/slike/water.png" style="max-width:100%;height:auto;">
        </div>
        <div id="documentation" class="tab-pane fade">
            <h3>Menu 3</h3>
            <p>Eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
                Eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
                Eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
                Eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
                Eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
                Eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
                Eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.</p>
            <embed src="../Documentation/Examples.pdf" style="max-width:100%;height:auto;background-color:lightblue;" />
            <br>
            <embed src="Examples.pdf" style="max-width:100%;height:auto;background-color:lightblue;" />
        </div>
    </div>
</div>


</body>
</html>
