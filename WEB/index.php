<!DOCTYPE html>
<html lang="en">
<head>
    <title>Bootstrap Example</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
    <script src="js/ModifyLogic.js"></script>

</head>
<body>

<div class="jumbotron text-center" style="margin-bottom:0">
    <h1>S2 <small>tool</small></h1>
    <p>WEB part of CLI</p>
</div>

<div class="container">
    <ul class="nav nav-tabs bg-dark ">
        <li ><a data-toggle="tab" href="#home">Home</a></li>
        <li class="active"><a data-toggle="tab" href="#modify">Modify</a></li>
        <li><a data-toggle="tab" href="#generate">Generate</a></li>
        <li><a data-toggle="tab" href="#documentation">Documentation</a></li>
    </ul>

    <div class="tab-content">
        <div id="home" class="tab-pane fade in ">
            <h3>README</h3>
            <p>Web interfance off CLI project</p>
            <h3>DEPENDENCIES</h3>
            <blockquote>Finding prooper dependencies is left for the reader as an exercise!!!
                <footer>lazy developer</footer>
            </blockquote>
            <h3>LICENSE THEOREM</h3>
            <p>There exist such license that this project is suitable to that exact license.</p>

        </div>
        <div id="modify" class="tab-pane fade in active">

            <form method="post" enctype="multipart/form-data">
                <div id="divUF1">
                    <h3>Upload File</h3>
                    <label class="btn btn-primary btn-file">
                        Browse <input type="file" id="inputF1" style="display: none;" accept=".s2" name="file1" required>
                    </label>
                    <small class="form-text text-muted">Select the file you want to modify.</small>
                    <br>
                    <input type="submit" class="btn-primary" value="Upload File" name="submit">
                </div>

                <!--DIV chosing actions-->


            </form>
        </div>
        <div id="generate" class="tab-pane fade">
            <h3>Menu 2</h3>
            <p><?php echo date("U");?></p>
            <p>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam.</p>
            <img src="DocDir.php?file=tree.jpg" style="max-width:100%;height:auto;">
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
            <embed src="DocDir.php?file=Examples.pdf" style="max-width:100%;width:100%;height:600px;background-color:lightblue;" />
        </div>
    </div>
</div>


</body>
</html>
