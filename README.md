# Transfer Commands
This fabric mod adds commands for using the fabric transfer api. 
This can be useful for testing integration in mods and for datapacks to easily interact with modded containers

## Usage
All commands are under the `/transfer` command and require permission level 2 (same as `/give`). 
If you need to integrate this with a permission management system you can use [Universal Perms](https://github.com/MattiDragon/universal-perms).

### Insert
The `/transfer insert` command will allow you to insert resources into a container. 
You have to specify the type of resource (fluid, item or energy) and a storage to insert to using a position and a side.
Next, you specify the maximum amount of resources you want to insert, storages are free to only accept some of it.
For items and fluids you also have to specify a type to insert. 

You can additionally specify the `simulate` flag to only simulate the insertion and not actually change anything.
If you are a mod dev: make sure you support simulating by using snapshots or a premade storage supporting it.

The command will return the amount of resources successfully inserted or simulated.

## Extract
The `/transfer extract` command works almost identically to `/transfer insert` except that it removes items instead of adding them.

## Move
The `/transfer move` command is the most powerful of the transfer commands. 
It has similar syntax to the other two, but takes two storages instead of one. First the source and then the destination.
It also allows replacing the amount and type values with `*` to allow infinite resources and any resources, respectively.