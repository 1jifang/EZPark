<div id="crowd-meter" style="width: 100%; background-color: #f3f3f3;">
    <div id="crowd-bar" style="width: 0%; height: 30px; background-color: #4caf50;"></div>
</div>

<script>
    async function updateCrowdMeter(streetName) {
        try {
            let response = await fetch(`http://localhost:5000/crowd-meter/${streetName}`);
            if (response.ok) {
                let data = await response.json();
                let crowdMetric = data.crowd_metric;

                // Update the progress bar width and color based on crowdMetric
                let crowdBar = document.getElementById('crowd-bar');
                crowdBar.style.width = crowdMetric + '%';
                
                // Change color based on availability
                if (crowdMetric > 60) {
                    crowdBar.style.backgroundColor = '#4caf50'; // Green for high availability
                } else if (crowdMetric > 30) {
                    crowdBar.style.backgroundColor = '#ffeb3b'; // Yellow for moderate availability
                } else {
                    crowdBar.style.backgroundColor = '#f44336'; // Red for low availability
                }
            } else {
                console.error("Failed to fetch data");
            }
        } catch (error) {
            console.error("Error fetching crowd data: ", error);
        }
    }

    // Update the crowd meter for a given street name
    updateCrowdMeter('Street A');
</script>


const express = require('express');
const mongoose = require('mongoose');
const app = express();
const PORT = process.env.PORT || 5000;

// Connect to MongoDB
mongoose.connect('mongodb://localhost:27017/ezpark', {
  useNewUrlParser: true,
  useUnifiedTopology: true,
});

// Define a schema for parking spots
const parkingSchema = new mongoose.Schema({
  street: String,
  totalSpots: Number,
  availableSpots: Number,
  updatedAt: { type: Date, default: Date.now }
});

const ParkingSpot = mongoose.model('ParkingSpot', parkingSchema);

// Middleware for parsing JSON
app.use(express.json());

// Get the crowd metric for a specific street
app.get('/crowd-meter/:street', async (req, res) => {
  try {
    const streetName = req.params.street;
    const parkingData = await ParkingSpot.findOne({ street: streetName });

    if (!parkingData) {
      return res.status(404).json({ error: "Street not found" });
    }

    const { availableSpots, totalSpots } = parkingData;
    const crowdMetric = (availableSpots / totalSpots) * 100;

    res.json({
      street: streetName,
      availableSpots,
      totalSpots,
      crowdMetric,
    });
  } catch (error) {
    res.status(500).json({ error: "Internal Server Error" });
  }
});

// Update available spots for a street
app.put('/update-spot/:street', async (req, res) => {
  try {
    const streetName = req.params.street;
    const { availableSpots } = req.body;

    const parkingData = await ParkingSpot.findOneAndUpdate(
      { street: streetName },
      { availableSpots, updatedAt: Date.now() },
      { new: true }
    );

    if (!parkingData) {
      return res.status(404).json({ error: "Street not found" });
    }

    res.json(parkingData);
  } catch (error) {
    res.status(500).json({ error: "Internal Server Error" });
  }
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
